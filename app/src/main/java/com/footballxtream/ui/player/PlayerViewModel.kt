package com.footballxtream.ui.player

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.StreamUrlBuilder
import com.footballxtream.data.XtreamRepository
import com.footballxtream.data.local.SettingsStore
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.Quality
import com.footballxtream.model.QualityMode
import com.footballxtream.player.PlaybackSession
import com.footballxtream.player.PlayerEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val channelName: String = "",
    val qualityLabel: String = "",
    val throughputMbps: Double = 0.0,
    val resolution: String? = null,
    val isBuffering: Boolean = true,
    val autoMode: Boolean = false,
)

@OptIn(UnstableApi::class)
class PlayerViewModel(
    private val playbackSession: PlaybackSession,
    private val settingsStore: SettingsStore,
    private val playerEngine: PlayerEngine,
    repository: XtreamRepository,
) : ViewModel() {

    private val group: ChannelGroup? = playbackSession.current
    private val profile = repository.currentProfile

    val canPlay: Boolean = group != null && profile != null

    val player: ExoPlayer = playerEngine.build()

    private val _ui = MutableStateFlow(PlayerUiState())
    val ui: StateFlow<PlayerUiState> = _ui.asStateFlow()

    private var currentQuality: Quality = Quality.UNKNOWN
    private var rebufferCount = 0
    private var hasStartedOnce = false

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _ui.update { it.copy(isBuffering = state == Player.STATE_BUFFERING) }
            when (state) {
                Player.STATE_READY -> {
                    hasStartedOnce = true
                    rebufferCount = 0
                }
                Player.STATE_BUFFERING -> {
                    if (hasStartedOnce && _ui.value.autoMode) {
                        rebufferCount++
                        if (rebufferCount >= 3) downshift()
                    }
                }
            }
        }
    }

    init {
        val g = group
        if (g != null && profile != null) {
            viewModelScope.launch {
                val mode = settingsStore.qualityMode.first()
                val variant = chooseVariant(g, mode)
                currentQuality = variant.quality
                _ui.update {
                    it.copy(
                        channelName = g.displayName,
                        qualityLabel = variant.quality.label,
                        autoMode = mode == QualityMode.AUTO,
                    )
                }
                player.addListener(listener)
                playVariantStream(g, variant.quality)
                pollStats()
            }
        }
    }

    private fun playVariantStream(g: ChannelGroup, quality: Quality) {
        val p = profile ?: return
        val variant = g.variantFor(quality) ?: g.bestVariant()
        val url = StreamUrlBuilder.liveUrl(p, variant.channel.streamId)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    private fun downshift() {
        val g = group ?: return
        val lower = g.variants.firstOrNull { it.quality.rank < currentQuality.rank } ?: return
        rebufferCount = 0
        currentQuality = lower.quality
        _ui.update { it.copy(qualityLabel = lower.quality.label) }
        playVariantStream(g, lower.quality)
    }

    private suspend fun chooseVariant(g: ChannelGroup, mode: QualityMode) =
        when {
            mode.fixedQuality != null ->
                g.variantFor(mode.fixedQuality!!) ?: g.variantAtOrBelow(mode.fixedQuality!!)
            mode == QualityMode.ALL -> g.bestVariant()
            else -> {
                val bandwidth = maxOf(settingsStore.bandwidthBps(), playerEngine.bitrateEstimateBps())
                if (bandwidth <= 0L) {
                    g.variantAtOrBelow(Quality.FHD)
                } else {
                    val budget = (bandwidth * 0.8).toLong()
                    g.variants.firstOrNull { it.quality.typicalBitrateBps <= budget }
                        ?: g.variants.last()
                }
            }
        }

    private fun pollStats() {
        viewModelScope.launch {
            var tick = 0
            while (isActive) {
                delay(1_000)
                val bps = playerEngine.bitrateEstimateBps()
                val format = player.videoFormat
                val resolution = format?.takeIf { it.width > 0 && it.height > 0 }
                    ?.let { "${it.width}x${it.height}" }
                _ui.update {
                    it.copy(throughputMbps = bps / 1_000_000.0, resolution = resolution)
                }
                if (++tick % 5 == 0) settingsStore.setBandwidthBps(bps)
            }
        }
    }

    override fun onCleared() {
        player.removeListener(listener)
        player.release()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                PlayerViewModel(
                    container.playbackSession,
                    container.settingsStore,
                    container.playerEngine,
                    container.repository,
                )
            }
        }
    }
}
