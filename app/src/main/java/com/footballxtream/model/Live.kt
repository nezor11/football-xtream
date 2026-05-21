package com.footballxtream.model

data class ChannelCategory(
    val id: String,
    val name: String,
)

data class LiveChannel(
    val streamId: Int,
    val name: String,
    val iconUrl: String?,
    val categoryId: String?,
    val epgChannelId: String?,
    val number: Int?,
)
