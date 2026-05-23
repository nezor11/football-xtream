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
) : ViewModel() {

    val canPlay: Boolean = playbackSession.current != null

    val player: ExoPlayer = playerEngine.build()

    private val _ui = MutableStateFlow(PlayerUiState())
    val ui: StateFlow<PlayerUiState> = _ui.asStateFlow()

    private var qualityMode: QualityMode = QualityMode.AUTO
    private var currentGroup: ChannelGroup? = null
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
                    if (hasStartedOnce && qualityMode == QualityMode.AUTO) {
                        rebufferCount++
                        if (rebufferCount >= 3) downshift()
                    }
                }
            }
        }
    }

    init {
        val first = playbackSession.current
        if (first != null) {
            player.addListener(listener)
            viewModelScope.launch {
                qualityMode = settingsStore.qualityMode.first()
                _ui.update { it.copy(autoMode = qualityMode == QualityMode.AUTO) }
                playGroup(first)
                pollStats()
            }
        }
    }

    fun nextChannel() {
        val group = playbackSession.next() ?: return
        viewModelScope.launch { playGroup(group) }
    }

    fun previousChannel() {
        val group = playbackSession.previous() ?: return
        viewModelScope.launch { playGroup(group) }
    }

    private suspend fun playGroup(group: ChannelGroup) {
        currentGroup = group
        rebufferCount = 0
        hasStartedOnce = false
        val variant = chooseVariant(group, qualityMode)
        currentQuality = variant.quality
        _ui.update {
            it.copy(
                channelName = group.displayName,
                qualityLabel = variant.quality.label,
                isBuffering = true,
            )
        }
        playVariant(group, variant.quality)
    }

    private fun playVariant(group: ChannelGroup, quality: Quality) {
        val variant = group.variantFor(quality) ?: group.bestVariant()
        player.setMediaItem(MediaItem.fromUri(variant.channel.streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    private fun downshift() {
        val group = currentGroup ?: return
        val lower = group.variants.firstOrNull { it.quality.rank < currentQuality.rank } ?: return
        rebufferCount = 0
        currentQuality = lower.quality
        _ui.update { it.copy(qualityLabel = lower.quality.label) }
        playVariant(group, lower.quality)
    }

    private suspend fun chooseVariant(group: ChannelGroup, mode: QualityMode) =
        when {
            mode.fixedQuality != null ->
                group.variantFor(mode.fixedQuality!!) ?: group.variantAtOrBelow(mode.fixedQuality!!)
            mode == QualityMode.ALL -> group.bestVariant()
            else -> {
                val bandwidth = maxOf(settingsStore.bandwidthBps(), playerEngine.bitrateEstimateBps())
                if (bandwidth <= 0L) {
                    group.variantAtOrBelow(Quality.FHD)
                } else {
                    val budget = (bandwidth * 0.8).toLong()
                    group.variants.firstOrNull { it.quality.typicalBitrateBps <= budget }
                        ?: group.variants.last()
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
                )
            }
        }
    }
}
