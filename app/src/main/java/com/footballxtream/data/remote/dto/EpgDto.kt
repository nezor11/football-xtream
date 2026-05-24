package com.footballxtream.data.remote.dto

import android.util.Base64
import com.footballxtream.model.EpgProgram
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of player_api.php?action=get_short_epg. Titles/descriptions are base64-encoded. */
@Serializable
data class ShortEpgResponse(
    @SerialName("epg_listings") val epgListings: List<EpgListingDto> = emptyList(),
)

@Serializable
data class EpgListingDto(
    val title: String? = null,
    // Xtream usually returns these as strings; kept nullable + parsed defensively.
    @SerialName("start_timestamp") val startTimestamp: String? = null,
    @SerialName("stop_timestamp") val stopTimestamp: String? = null,
    @SerialName("now_playing") val nowPlaying: Int? = null,
) {
    fun toModel(): EpgProgram? {
        val decoded = decodeBase64(title)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return EpgProgram(
            title = decoded,
            start = (startTimestamp?.toLongOrNull() ?: 0L) * 1000,
            end = (stopTimestamp?.toLongOrNull() ?: 0L) * 1000,
            nowFlag = nowPlaying == 1,
        )
    }

    private fun decodeBase64(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching { String(Base64.decode(value, Base64.DEFAULT)) }.getOrNull()
    }
}
