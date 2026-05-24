package com.footballxtream.model

import kotlinx.serialization.Serializable

/**
 * A brand/family that bundles its numbered channels, e.g. "beIN Sports" → [beIN Sports 1, 2, 3...].
 * A single-channel family is just a folder with one channel (shown as a normal channel card).
 */
@Serializable
data class ChannelFolder(
    val name: String,
    val iconUrl: String?,
    val isFootball: Boolean,
    val channels: List<ChannelGroup>,
) {
    val isSingle: Boolean get() = channels.size == 1
    val single: ChannelGroup get() = channels.first()
}
