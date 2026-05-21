package com.footballxtream.data.remote.dto

import com.footballxtream.model.ChannelCategory
import com.footballxtream.model.LiveChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveCategoryDto(
    @SerialName("category_id") val categoryId: String = "",
    @SerialName("category_name") val categoryName: String = "",
) {
    fun toModel() = ChannelCategory(id = categoryId, name = categoryName.trim())
}

@Serializable
data class LiveStreamDto(
    // num/stream_id come as int on most panels but string on a few; keep as String to be safe.
    val num: String? = null,
    val name: String = "",
    @SerialName("stream_id") val streamId: Int = 0,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("stream_type") val streamType: String? = null,
) {
    fun toModel() = LiveChannel(
        streamId = streamId,
        name = name.trim(),
        iconUrl = streamIcon?.takeIf { it.isNotBlank() },
        categoryId = categoryId,
        epgChannelId = epgChannelId?.takeIf { it.isNotBlank() },
        number = num?.toIntOrNull(),
    )
}
