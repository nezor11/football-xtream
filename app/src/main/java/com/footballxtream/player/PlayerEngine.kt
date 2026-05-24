package com.footballxtream.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.footballxtream.data.local.SettingsStore
import com.footballxtream.data.remote.XtreamClient
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
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

    /**
     * Renderers backed by the NextLib FFmpeg extension. EXTENSION_RENDERER_MODE_ON keeps the
     * device's hardware decoders first (e.g. AAC, H.264) and falls back to the software FFmpeg
     * decoders only when no hardware one exists — which is the case for the AC-3 / E-AC-3 (Dolby)
     * and MP2 audio many IPTV channels (DAZN, Movistar…) use, that would otherwise be silent.
     */
    private fun renderersFactory(): DefaultRenderersFactory =
        NextRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

    fun build(): ExoPlayer =
        ExoPlayer.Builder(context, renderersFactory())
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build()

    /** Current network throughput estimate in bits per second. */
    fun bitrateEstimateBps(): Long = bandwidthMeter.bitrateEstimate
}
