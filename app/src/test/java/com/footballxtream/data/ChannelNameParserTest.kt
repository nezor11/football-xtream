package com.footballxtream.data

import com.footballxtream.model.Quality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelNameParserTest {

    // --- quality() ---

    @Test
    fun quality_spacedTags() {
        assertEquals(Quality.UHD_4K, ChannelNameParser.quality("Canal 4K"))
        assertEquals(Quality.QHD, ChannelNameParser.quality("Canal 2K"))
        assertEquals(Quality.FHD, ChannelNameParser.quality("Canal FHD"))
        assertEquals(Quality.FHD, ChannelNameParser.quality("Canal Full HD"))
        assertEquals(Quality.HD, ChannelNameParser.quality("Canal HD"))
        assertEquals(Quality.SD, ChannelNameParser.quality("Canal SD"))
        assertEquals(Quality.UNKNOWN, ChannelNameParser.quality("Canal sin etiqueta"))
    }

    @Test
    fun quality_gluedToWord() {
        assertEquals(Quality.HD, ChannelNameParser.quality("LaLigaTVHD"))
    }

    @Test
    fun quality_gluedWithTrailingFlagEmoji() {
        // Noise (flags/emoji) is stripped before quality is read, so the tag still resolves.
        assertEquals(Quality.HD, ChannelNameParser.quality("LigaSmartBankHD🇪🇸"))
    }

    @Test
    fun quality_doesNotReadNumberAsTag() {
        // "1080" must not be read as a quality just because it contains digits around HD/SD.
        assertEquals(Quality.FHD, ChannelNameParser.quality("Canal 1080"))
    }

    // --- baseName() / folderName() ---

    @Test
    fun baseName_stripsQualityAndPipeWrappedCountryPrefix() {
        assertEquals("Sky Sport News", ChannelNameParser.baseName("|DE|  Sky Sport News HD"))
        assertEquals("Sky Sport 24", ChannelNameParser.baseName("|IT| Sky Sport 24 HD"))
    }

    @Test
    fun baseName_keepsPlainTwoLetterWords() {
        // Without a separator a leading short word is part of the name, never a prefix.
        assertEquals("La Liga TV", ChannelNameParser.baseName("La Liga TV"))
        assertEquals("Al Jazeera Sport", ChannelNameParser.baseName("Al Jazeera Sport"))
    }

    @Test
    fun folderName_dropsTrailingChannelNumber() {
        assertEquals("beIN Sports", ChannelNameParser.folderName("beIN Sports 1"))
        assertEquals("Movistar LaLiga", ChannelNameParser.folderName("Movistar LaLiga"))
    }

    // --- isSports() ---

    @Test
    fun isSports_nameAlwaysWins() {
        assertTrue(ChannelNameParser.isSports("beIN Sports NEWS", null))
        // A "sport" in the name keeps it even if the name is also a known general broadcaster.
        assertTrue(ChannelNameParser.isSports("Al Jazeera Sport", null))
    }

    @Test
    fun isSports_categoryCountsUnlessGeneralBroadcaster() {
        assertTrue(ChannelNameParser.isSports("La 1", "Deportes"))
        // Mis-filed general/news channel inside a sports category is dropped.
        assertFalse(ChannelNameParser.isSports("2M Maroc", "BEINSPORT"))
    }

    @Test
    fun isSports_nonSportRejected() {
        assertFalse(ChannelNameParser.isSports("CNN", "Noticias"))
        assertFalse(ChannelNameParser.isSports("Película", "Cine"))
    }

    // --- isFootball() ---

    @Test
    fun isFootball() {
        assertTrue(ChannelNameParser.isFootball("LaLiga TV", null))
        assertTrue(ChannelNameParser.isFootball("beIN Sports 1", null))
        assertFalse(ChannelNameParser.isFootball("NBA TV", null))
    }

    // --- isVodCategory() ---

    @Test
    fun isVodCategory_wholeWordOnly() {
        assertTrue(ChannelNameParser.isVodCategory("VOD FR"))
        assertFalse(ChannelNameParser.isVodCategory("VODAFONE"))
        assertFalse(ChannelNameParser.isVodCategory("Deportes"))
        assertFalse(ChannelNameParser.isVodCategory(null))
    }
}
