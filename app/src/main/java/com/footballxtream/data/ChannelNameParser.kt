package com.footballxtream.data

import com.footballxtream.model.Quality

/**
 * Heuristics to make sense of the wildly inconsistent channel naming used by IPTV panels:
 * extract a quality tier, normalize the base channel name (so duplicate-quality streams collapse
 * into one logical channel), and classify sports / football content.
 */
object ChannelNameParser {

    // Order matters: 4K before FHD, FHD before HD ("FULL HD" contains "HD").
    private val qualityPatterns: List<Pair<Quality, Regex>> = listOf(
        Quality.UHD_4K to Regex("""(?<![a-z0-9])(4k|uhd|2160p?)(?![a-z0-9])""", RegexOption.IGNORE_CASE),
        Quality.FHD to Regex("""(?<![a-z0-9])(fhd|full\s*hd|1080p?)(?![a-z0-9])""", RegexOption.IGNORE_CASE),
        Quality.HD to Regex("""(?<![a-z0-9])(hd|720p?)(?![a-z0-9])""", RegexOption.IGNORE_CASE),
        Quality.SD to Regex("""(?<![a-z0-9])(sd|480p?|360p?)(?![a-z0-9])""", RegexOption.IGNORE_CASE),
    )

    private val allQualityTokens = Regex(
        """(?<![a-z0-9])(4k|uhd|2160p?|fhd|full\s*hd|1080p?|hd|720p?|sd|480p?|360p?)(?![a-z0-9])""",
        RegexOption.IGNORE_CASE,
    )

    // Leading provider/country prefix like "ES|", "ES:", "EN -", "VIP >".
    private val leadingPrefix = Regex("""^\s*[\p{L}0-9]{1,4}\s*[|:>\-•]\s*""")
    private val bracketed = Regex("""[\[(][^\])]*[\])]""")
    // Strip emoji / flags / leftover symbol noise (keep letters, numbers, spaces, basic punctuation).
    private val noise = Regex("""[^\p{L}\p{N}\s&+./'-]""")
    private val multiSpace = Regex("""\s{2,}""")

    private val sportsKeywords = listOf(
        // Generic "sport" in many languages
        "sport", "deporte", "esporte", "esport", "spor", "sportv", "رياضة", "رياضي", "اسبور",
        "спорт", "체육", "运动", "體育",
        // Football / soccer (multi-language)
        "futbol", "fútbol", "football", "soccer", "fußball", "fussball", "calcio", "futebol",
        "voetbal", "fudbal", "ποδόσφαιρο", "كرة",
        // Leagues & competitions
        "laliga", "la liga", "liga", "ligue", "premier league", "championship", "bundesliga",
        "eredivisie", "serie a", "serie b", "primeira", "champions", "uefa", "europa league",
        "conference league", "copa", "libertadores", "sudamericana", "fa cup", "efl", "mls",
        "super lig", "jupiler", "superliga", "eliteserien", "allsvenskan", "fifa", "uefa",
        // Sports channel brands
        "dazn", "bein", "espn", "fox sport", "sky sport", "tnt sport", "bt sport",
        "movistar deport", "movistar liga", "movistar campeones", "movistar laliga", "gol ",
        "goltv", "eurosport", "tudn", "tyc", "ssc ", "rmc sport", "canal+ sport", "canal plus sport",
        "supersport", "ssport", "digi sport", "arena sport", "sportklub", "nova sport", "sport tv",
        "sport1", "sportdigital", "premier sport", "viaplay", "setanta", "astro sport", "elclasico",
        "sportitalia", "primesport",
        // Basketball
        "baloncesto", "basket", "basketball", "nba", "acb", "euroleague", "euroliga", "basquet",
        // Tennis
        "tennis", "tenis", "atp", "wta", "roland garros", "wimbledon",
        // Motorsport
        "f1 ", "formula 1", "fórmula 1", "formula1", "motogp", "moto gp", "nascar", "indycar",
        "wrc", "dakar", "superbike", "motor",
        // US / combat sports
        "nfl", "nhl", "mlb", "ufc", "wwe", "boxing", "boxeo", "boxe", "mma", "wrestling", "fight",
        "combat", "lucha",
        // Other sports
        "rugby", "cricket", "golf", "ciclismo", "cycling", "hockey", "handball", "balonmano",
        "voleibol", "volleyball", "volley", "atletismo", "athletics", "padel", "pádel", "snooker",
        "darts", "racing", "olympic", "olimpic", "juegos olimpicos", "extreme",
    )

    private val footballKeywords = listOf(
        "futbol", "fútbol", "football", "soccer", "fußball", "fussball", "calcio", "futebol",
        "voetbal", "fudbal", "ποδόσφαιρο",
        "laliga", "la liga", "premier league", "championship", "bundesliga", "eredivisie",
        "serie a", "serie b", "primeira liga", "ligue 1", "ligue 2",
        "champions", "uefa", "europa league", "conference league", "copa del rey", "copa",
        "libertadores", "sudamericana", "fa cup", "efl", "mls", "super lig", "jupiler", "fifa",
        "bein sport", "dazn laliga", "movistar liga", "movistar campeones", "movistar laliga",
        "gol ", "goltv", "tudn", "premier sport",
    )

    fun quality(rawName: String): Quality {
        for ((quality, pattern) in qualityPatterns) {
            if (pattern.containsMatchIn(rawName)) return quality
        }
        return Quality.UNKNOWN
    }

    /** Canonical channel name with quality tags, prefixes and noise removed. */
    fun baseName(rawName: String): String {
        var name = rawName
        name = bracketed.replace(name, " ")
        name = leadingPrefix.replace(name, "")
        name = allQualityTokens.replace(name, " ")
        name = noise.replace(name, " ")
        name = multiSpace.replace(name, " ").trim().trim('-', '|', ':', '.', '·').trim()
        return name
    }

    /** Stable identity used to group quality variants of the same channel. */
    fun groupKey(rawName: String): String = baseName(rawName).lowercase()

    fun isSports(channelName: String, categoryName: String?): Boolean =
        matchesAny(channelName, sportsKeywords) || matchesAny(categoryName, sportsKeywords)

    fun isFootball(channelName: String, categoryName: String?): Boolean =
        matchesAny(channelName, footballKeywords) || matchesAny(categoryName, footballKeywords)

    private fun matchesAny(text: String?, keywords: List<String>): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase()
        return keywords.any { lower.contains(it) }
    }
}
