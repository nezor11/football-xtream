package com.footballxtream.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.footballxtream.data.local.SettingsStore
import com.footballxtream.data.remote.XtreamClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Creates ExoPlayer instances tuned for IPTV: a shared bandwidth meter (so the Auto quality logic
 * can read a real throughput estimate) and a load control with generous pre-buffering to smooth out
 * the jittery delivery typical of Xtream live streams.
 *
 * Uses an OkHttp-backed data source so multi-hop, cross-host 302 redirects (common in IPTV CDNs
 * with tokenized URLs) are followed exactly like a normal client; the framework DefaultHttpDataSource
 * mishandled those chains and returned 401.
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

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", XtreamClient.USER_AGENT)
                    .build(),
            )
        }
        .build()

    private val httpDataSourceFactory: OkHttpDataSource.Factory =
        OkHttpDataSource.Factory(okHttpClient)

    fun build(): ExoPlayer =
        ExoPlayer.Builder(context)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build()

    /** Current network throughput estimate in bits per second. */
    fun bitrateEstimateBps(): Long = bandwidthMeter.bitrateEstimate
}
