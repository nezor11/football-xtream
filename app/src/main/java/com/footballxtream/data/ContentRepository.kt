package com.footballxtream.data

import android.util.Log
import com.footballxtream.data.remote.XtreamApi
import com.footballxtream.data.remote.XtreamClient
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.LiveChannel
import com.footballxtream.model.XtreamProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class XtreamAuthException : Exception("Autenticación rechazada por el servidor")

/**
 * Single entry point for channel data. Holds the active source (Xtream API or an M3U playlist)
 * and exposes sports-grouped channels regardless of the source.
 */
class ContentRepository {

    private sealed interface Binding {
        data class Xtream(val profile: XtreamProfile, val api: XtreamApi) : Binding
        data class M3u(val url: String) : Binding
    }

    @Volatile
    private var binding: Binding? = null

    // --- Restore a saved profile without a network round-trip ---

    fun bindXtream(profile: XtreamProfile) {
        binding = Binding.Xtream(profile, XtreamClient.create(profile.serverUrl))
    }

    fun bindM3u(url: String) {
        binding = Binding.M3u(url.trim())
    }

    // --- Validate credentials/URL when adding a profile (binds on success) ---

    suspend fun validateXtream(profile: XtreamProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val api = XtreamClient.create(profile.serverUrl)
            val response = api.login(profile.username, profile.password)
            val info = response.userInfo
            if (info == null || info.auth != 1) throw XtreamAuthException()
            binding = Binding.Xtream(profile, api)
        }
    }

    suspend fun validateM3u(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val firstLine = XtreamClient.fetchFirstLine(url)
            if (!firstLine.trimStart().startsWith("#EXTM3U", ignoreCase = true)) {
                throw IOException("La URL no devuelve una lista M3U válida (inicio: ${firstLine.take(60)})")
            }
            binding = Binding.M3u(url.trim())
        }.onFailure { Log.w(TAG, "validateM3u failed for $url", it) }
    }

    // --- Load the sports channel groups for the active source ---

    suspend fun loadLiveGroups(): Result<List<ChannelGroup>> = withContext(Dispatchers.IO) {
        runCatching {
            when (val current = binding) {
                is Binding.Xtream -> loadXtream(current)
                is Binding.M3u -> ChannelGrouping.build(M3uParser.parse(XtreamClient.fetchText(current.url)))
                null -> error("No active content source")
            }
        }.onFailure { Log.w(TAG, "loadLiveGroups failed", it) }
    }

    private suspend fun loadXtream(binding: Binding.Xtream): List<ChannelGroup> {
        val profile = binding.profile
        val api = binding.api
        val categoryName = api.getLiveCategories(profile.username, profile.password)
            .associate { it.categoryId to it.categoryName.trim() }
        val channels = api.getLiveStreams(profile.username, profile.password).map { stream ->
            LiveChannel(
                streamId = stream.streamId,
                name = stream.name.trim(),
                iconUrl = stream.streamIcon?.takeIf { it.isNotBlank() },
                categoryName = categoryName[stream.categoryId],
                streamUrl = StreamUrlBuilder.liveUrl(profile, stream.streamId),
            )
        }
        return ChannelGrouping.build(channels)
    }

    private companion object {
        const val TAG = "FXContent"
    }
}
