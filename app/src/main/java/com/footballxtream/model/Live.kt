package com.footballxtream.model

/** A category from the Xtream API (used only while parsing Xtream responses). */
data class ChannelCategory(
    val id: String,
    val name: String,
)

/**
 * A single playable live channel, independent of the source (Xtream API or M3U playlist).
 * [streamUrl] is the directly playable URL; [streamId] is a stable id used as the favorites key.
 */
data class LiveChannel(
    val streamId: Int,
    val name: String,
    val iconUrl: String?,
    val categoryName: String?,
    val streamUrl: String,
)
