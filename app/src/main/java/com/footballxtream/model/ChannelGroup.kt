package com.footballxtream.model

import kotlinx.serialization.Serializable

@Serializable
data class ChannelVariant(
    val channel: LiveChannel,
    val quality: Quality,
)

/** A logical channel that bundles its per-quality stream variants. */
@Serializable
data class ChannelGroup(
    val key: String,
    val displayName: String,
    val iconUrl: String?,
    val isFootball: Boolean,
    val variants: List<ChannelVariant>,
    /** XMLTV channel id of the representative variant, for M3U guide lookup. */
    val epgId: String? = null,
) {
    val availableQualities: Set<Quality> = variants.map { it.quality }.toSet()

    fun bestVariant(): ChannelVariant = variants.first()

    fun variantFor(quality: Quality): ChannelVariant? =
        variants.firstOrNull { it.quality == quality }

    /** Highest available variant at or below [quality]; falls back to the lowest available. */
    fun variantAtOrBelow(quality: Quality): ChannelVariant =
        variants.firstOrNull { it.quality.rank <= quality.rank } ?: variants.last()
}
