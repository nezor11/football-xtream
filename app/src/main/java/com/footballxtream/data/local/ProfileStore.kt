package com.footballxtream.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.footballxtream.model.XtreamProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "profile")

/**
 * Persists the active Xtream profile. Credentials are stored as-is for the user's own
 * account on their own device; encrypting at rest is a future hardening step.
 */
class ProfileStore(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("name")
        val SERVER = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
    }

    val profile: Flow<XtreamProfile?> = context.dataStore.data.map { prefs ->
        val server = prefs[Keys.SERVER]
        val username = prefs[Keys.USERNAME]
        val password = prefs[Keys.PASSWORD]
        if (server.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
            null
        } else {
            XtreamProfile(
                name = prefs[Keys.NAME].orEmpty(),
                serverUrl = server,
                username = username,
                password = password,
            )
        }
    }

    suspend fun save(profile: XtreamProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name
            prefs[Keys.SERVER] = profile.serverUrl
            prefs[Keys.USERNAME] = profile.username
            prefs[Keys.PASSWORD] = profile.password
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
