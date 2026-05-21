package com.footballxtream.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.footballxtream.data.local.SettingsStore

/**
 * Creates ExoPlayer instances tuned for IPTV: a shared bandwidth meter (so the Auto quality logic
 * can read a real throughput estimate) and a load control with generous pre-buffering to smooth out
 * the jittery delivery typical of Xtream live streams.
 */
@OptIn(UnstableApi::class)
class PlayerEngine(
    private val context: Context,
    private val settingsStore: SettingsStore,
) {
    val bandwidthMeter: DefaultBandwidthMeter = DefaultBandwidthMeter.Builder(context).build()

    private val loadControl: DefaultLoadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 20_000,
            /* maxBufferMs = */ 60_000,
            /* bufferForPlaybackMs = */ 3_000,
            /* bufferForPlaybackAfterRebufferMs = */ 6_000,
        )
        .build()

    fun build(): ExoPlayer =
        ExoPlayer.Builder(context)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .build()

    /** Current network throughput estimate in bits per second. */
    fun bitrateEstimateBps(): Long = bandwidthMeter.bitrateEstimate
}
