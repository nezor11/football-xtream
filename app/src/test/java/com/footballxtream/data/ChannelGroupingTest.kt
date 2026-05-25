package com.footballxtream.data

import com.footballxtream.model.LiveChannel
import com.footballxtream.model.Quality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelGroupingTest {

    private fun channel(name: String, category: String?, epgId: String? = null) =
        LiveChannel(
            streamId = name.hashCode(),
            name = name,
            iconUrl = null,
            categoryName = category,
            streamUrl = "http://host/${name.hashCode()}",
            epgId = epgId,
        )

    @Test
    fun build_mergesQualityVariantsOfSameChannel() {
        val groups = ChannelGrouping.build(
            listOf(
                channel("Movistar LaLiga HD", "Sports", epgId = "b1"),
                channel("Movistar LaLiga SD", "Sports", epgId = "b1"),
            ),
        )
        assertEquals(1, groups.size)
        val g = groups.first()
        assertEquals("Movistar LaLiga", g.displayName)
        // Variants sorted highest quality first.
        assertEquals(listOf(Quality.HD, Quality.SD), g.variants.map { it.quality })
        assertEquals(setOf(Quality.HD, Quality.SD), g.availableQualities)
        assertEquals("b1", g.epgId)
        assertTrue(g.isFootball) // via the "laliga" competition term, not any brand
    }

    @Test
    fun build_dropsNonSportsAndVod() {
        val groups = ChannelGrouping.build(
            listOf(
                channel("beIN Sports 1", "Sports"),
                channel("CNN", "News"),
                channel("Película Top", "VOD Movies"), // sports? no, and VOD category anyway
                channel("Liga en directo", "VOD Sports"), // VOD category is dropped even if sporty
            ),
        )
        assertEquals(1, groups.size)
        assertEquals("beIN Sports 1", groups.first().displayName)
    }

    @Test
    fun build_separatesDifferentChannelNumbers() {
        val groups = ChannelGrouping.build(
            listOf(
                channel("beIN Sports 1 HD", "Sports"),
                channel("beIN Sports 2 HD", "Sports"),
            ),
        )
        assertEquals(2, groups.size)
    }
}
