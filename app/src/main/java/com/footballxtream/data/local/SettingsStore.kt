package com.footballxtream.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.footballxtream.model.QualityMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val QUALITY_MODE = stringPreferencesKey("quality_mode")
        val BANDWIDTH_BPS = longPreferencesKey("bandwidth_bps")
    }

    val qualityMode: Flow<QualityMode> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.QUALITY_MODE]?.let { runCatching { QualityMode.valueOf(it) }.getOrNull() }
            ?: QualityMode.AUTO
    }

    suspend fun setQualityMode(mode: QualityMode) {
        context.settingsDataStore.edit { it[Keys.QUALITY_MODE] = mode.name }
    }

    /** Last measured network throughput in bits/sec, 0 if never measured. */
    suspend fun bandwidthBps(): Long =
        context.settingsDataStore.data.first()[Keys.BANDWIDTH_BPS] ?: 0L

    suspend fun setBandwidthBps(bps: Long) {
        if (bps <= 0) return
        context.settingsDataStore.edit { it[Keys.BANDWIDTH_BPS] = bps }
    }
}
