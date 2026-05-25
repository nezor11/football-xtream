package com.footballxtream.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    private val playlist = """
        #EXTM3U x-tvg-url="http://epg.example/guide.xml.gz"
        #EXTINF:-1 tvg-id="bein1.es" tvg-logo="http://logo/bein.png" group-title="Sports",beIN Sports 1 HD
        http://host/live/bein1.ts
        #EXTINF:-1 group-title="Movies",Una Película
        http://host/movie/123.mp4
        #EXTINF:-1 group-title="VOD",Otra
        http://host/stream/9.mkv
    """.trimIndent()

    @Test
    fun parse_keepsLiveChannelsAndCapturesAttributes() {
        val channels = M3uParser.parse(playlist)
        // The movie (/movie/ + .mp4) and the .mkv VOD-group entry are dropped; only beIN remains.
        assertEquals(1, channels.size)
        val bein = channels.first()
        assertEquals("beIN Sports 1 HD", bein.name)
        assertEquals("bein1.es", bein.epgId)
        assertEquals("http://logo/bein.png", bein.iconUrl)
        assertEquals("Sports", bein.categoryName)
        assertEquals("http://host/live/bein1.ts", bein.streamUrl)
    }

    @Test
    fun parse_usesTrailingNameWhenNoTvgName() {
        val channels = M3uParser.parse(playlist)
        assertEquals("beIN Sports 1 HD", channels.first().name)
    }

    @Test
    fun parse_channelWithoutTvgIdHasNullEpgId() {
        val content = """
            #EXTM3U
            #EXTINF:-1 group-title="Sports",Canal Deporte
            http://host/live/x.ts
        """.trimIndent()
        assertNull(M3uParser.parse(content).first().epgId)
    }

    @Test
    fun epgUrls_xTvgUrl() {
        assertEquals(listOf("http://epg.example/guide.xml.gz"), M3uParser.epgUrls(playlist))
    }

    @Test
    fun epgUrls_commaSeparatedUrlTvg() {
        val content = """#EXTM3U url-tvg="http://a.example/a.xml,http://b.example/b.xml.gz""""
        assertEquals(
            listOf("http://a.example/a.xml", "http://b.example/b.xml.gz"),
            M3uParser.epgUrls(content),
        )
    }

    @Test
    fun epgUrls_emptyWhenNoneDeclared() {
        assertTrue(M3uParser.epgUrls("#EXTM3U\n#EXTINF:-1,Foo\nhttp://x/y.ts").isEmpty())
    }
}
