package com.footballxtream.data

import com.footballxtream.data.remote.XtreamApi
import com.footballxtream.data.remote.XtreamClient
import com.footballxtream.model.ChannelCategory
import com.footballxtream.model.LiveChannel
import com.footballxtream.model.XtreamProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XtreamAuthException : Exception("Autenticación rechazada por el servidor")

/**
 * Single source of truth for Xtream calls. Holds the active session (profile + bound API)
 * once a login succeeds or a saved profile is restored at startup.
 */
class XtreamRepository {

    @Volatile
    private var api: XtreamApi? = null

    @Volatile
    var currentProfile: XtreamProfile? = null
        private set

    /** Restore a previously saved session without a network round-trip. */
    fun bind(profile: XtreamProfile) {
        currentProfile = profile
        api = XtreamClient.create(profile.serverUrl)
    }

    /** Validates credentials against player_api.php. Binds the session on success. */
    suspend fun login(profile: XtreamProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val freshApi = XtreamClient.create(profile.serverUrl)
            val response = freshApi.login(profile.username, profile.password)
            val info = response.userInfo
            if (info == null || info.auth != 1) throw XtreamAuthException()
            api = freshApi
            currentProfile = profile
        }
    }

    suspend fun liveCategories(): Result<List<ChannelCategory>> = withContext(Dispatchers.IO) {
        runCatching {
            val (api, profile) = session()
            api.getLiveCategories(profile.username, profile.password)
                .map { it.toModel() }
                .filter { it.name.isNotEmpty() }
        }
    }

    suspend fun liveStreams(categoryId: String? = null): Result<List<LiveChannel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (api, profile) = session()
                api.getLiveStreams(profile.username, profile.password, categoryId)
                    .map { it.toModel() }
            }
        }

    private fun session(): Pair<XtreamApi, XtreamProfile> {
        val boundApi = api
        val profile = currentProfile
        check(boundApi != null && profile != null) { "No active Xtream session" }
        return boundApi to profile
    }
}
