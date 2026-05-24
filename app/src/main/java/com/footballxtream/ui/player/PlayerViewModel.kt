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
import com.footballxtream.model.ChannelVariant
import com.footballxtream.model.Quality
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
    /** Currently selected emission type ("Auto" or a quality label) — shown as the overlay select. */
    val emissionLabel: String = "Auto",
    val throughputMbps: Double = 0.0,
    val resolution: String? = null,
    val isBuffering: Boolean = true,
    val qualityMenuOpen: Boolean = false,
    val qualityOptions: List<String> = emptyList(),
    val qualitySelectedIndex: Int = 0,
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

    private var currentGroup: ChannelGroup? = null
    private var currentQuality: Quality = Quality.UNKNOWN

    /**
     * Manual emission-type override chosen in the player; null = Auto (pick by measured network).
     * It is sticky: once the user fixes a quality, zapping to another channel keeps that choice.
     */
    private var selectedEmission: Quality? = null
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
                    // Only auto-adapt while on Auto; a manual emission type is respected as-is.
                    if (hasStartedOnce && selectedEmission == null) {
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
                // Seed the in-player select from the quality mode picked on the channels screen.
                selectedEmission = settingsStore.qualityMode.first().fixedQuality
                playGroup(first)
                pollStats()
            }
        }
    }

    // --- Channel zapping: all four arrows, only when the quality menu is closed ---

    fun nextChannel() {
        if (_ui.value.qualityMenuOpen) return
        val group = playbackSession.next() ?: return
        viewModelScope.launch { playGroup(group) }
    }

    fun previousChannel() {
        if (_ui.value.qualityMenuOpen) return
        val group = playbackSession.previous() ?: return
        viewModelScope.launch { playGroup(group) }
    }

    // --- Emission-type (quality) select, driven by OK ---

    fun openQualityMenu() {
        val group = currentGroup ?: return
        val options = emissionOptions(group)
        _ui.update {
            it.copy(
                qualityMenuOpen = true,
                qualityOptions = options.map(::emissionLabel),
                qualitySelectedIndex = options.indexOf(selectedEmission).coerceAtLeast(0),
            )
        }
    }

    fun closeQualityMenu() {
        _ui.update { it.copy(qualityMenuOpen = false) }
    }

    fun moveQualitySelection(delta: Int) {
        _ui.update {
            if (!it.qualityMenuOpen || it.qualityOptions.isEmpty()) {
                it
            } else {
                it.copy(
                    qualitySelectedIndex =
                        (it.qualitySelectedIndex + delta).coerceIn(0, it.qualityOptions.lastIndex),
                )
            }
        }
    }

    fun confirmQualitySelection() {
        val group = currentGroup ?: return
        if (!_ui.value.qualityMenuOpen) return
        val options = emissionOptions(group)
        selectedEmission = options.getOrNull(_ui.value.qualitySelectedIndex)
        rebufferCount = 0
        viewModelScope.launch {
            val variant = variantForEmission(group)
            currentQuality = variant.quality
            _ui.update {
                it.copy(
                    emissionLabel = emissionLabel(selectedEmission),
                    qualityMenuOpen = false,
                    isBuffering = true,
                )
            }
            playUri(variant)
        }
    }

    // --- Internals ---

    private suspend fun playGroup(group: ChannelGroup) {
        currentGroup = group
        rebufferCount = 0
        hasStartedOnce = false
        val variant = variantForEmission(group)
        currentQuality = variant.quality
        _ui.update {
            it.copy(
                channelName = group.displayName,
                emissionLabel = emissionLabel(selectedEmission),
                isBuffering = true,
                qualityMenuOpen = false,
            )
        }
        playUri(variant)
    }

    private fun playUri(variant: ChannelVariant) {
        player.setMediaItem(MediaItem.fromUri(variant.channel.streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    private fun downshift() {
        if (selectedEmission != null) return
        val group = currentGroup ?: return
        val lower = group.variants.firstOrNull { it.quality.rank < currentQuality.rank } ?: return
        rebufferCount = 0
        currentQuality = lower.quality
        playUri(group.variantFor(lower.quality) ?: group.bestVariant())
    }

    /**
     * Auto plus the channel's available qualities (high→low); the null entry represents Auto.
     * Deduplicated by label so tiers that share a label (e.g. SD and UNKNOWN both show "SD") collapse.
     */
    private fun emissionOptions(group: ChannelGroup): List<Quality?> =
        (listOf<Quality?>(null) + group.variants.map { it.quality })
            .distinctBy(::emissionLabel)

    private fun emissionLabel(quality: Quality?): String = quality?.label ?: "Auto"

    private suspend fun variantForEmission(group: ChannelGroup): ChannelVariant {
        val fixed = selectedEmission
        if (fixed != null) return group.variantFor(fixed) ?: group.variantAtOrBelow(fixed)
        // Auto: highest variant that fits the measured network.
        val bandwidth = maxOf(settingsStore.bandwidthBps(), playerEngine.bitrateEstimateBps())
        return if (bandwidth <= 0L) {
            group.variantAtOrBelow(Quality.FHD)
        } else {
            val budget = (bandwidth * 0.8).toLong()
            group.variants.firstOrNull { it.quality.typicalBitrateBps <= budget }
                ?: group.variants.last()
        }
    }

    private fun pollStats() {
        viewModelScope.launch {
            var tick = 0
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val bps = playerEngine.bitrateEstimateBps()
                val format = player.videoFormat
                val resolution = format?.takeIf { it.width > 0 && it.height > 0 }
                    ?.let { "${it.width}x${it.height}" }
                _ui.update {
                    it.copy(throughputMbps = bps / 1_000_000.0, resolution = resolution)
                }
                if (++tick % BANDWIDTH_PERSIST_EVERY == 0) settingsStore.setBandwidthBps(bps)
            }
        }
    }

    override fun onCleared() {
        player.removeListener(listener)
        player.release()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500L
        private const val BANDWIDTH_PERSIST_EVERY = 10 // persist the estimate ~every 5 s

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
