package com.footballxtream.data

import com.footballxtream.model.Quality

/**
 * Heuristics to make sense of the wildly inconsistent channel naming used by IPTV panels:
 * extract a quality tier, normalize the base channel name (so duplicate-quality streams collapse
 * into one logical channel), and classify sports / football content.
 */
object ChannelNameParser {

    // Order matters: 4K before 2K before FHD before HD ("FULL HD" contains "HD"). [tokenRegex] matches
    // a quality either delimited by non-alphanumerics ("LaLiga HD") or glued to the end of a word
    // ("LaLigaTVHD", "...HD+"), which IPTV panels do constantly.
    private val qualityPatterns: List<Pair<Quality, Regex>> = listOf(
        Quality.UHD_4K to tokenRegex("4k", "uhd", "2160p?"),
        Quality.QHD to tokenRegex("2k", "1440p?", "qhd"),
        Quality.FHD to tokenRegex("fhd", """full\s*hd""", "1080p?"),
        Quality.HD to tokenRegex("hd", "720p?"),
        Quality.SD to tokenRegex("sd", "480p?", "360p?"),
    )

    // A quality token matches whether it is spaced ("LaLiga HD") or glued to a word ("LaLigaTVHD"):
    // only the right side must be a boundary (end, space, symbol or emoji — anything non-alphanumeric),
    // and it must not start mid-number (so "1080" isn't read as "108"+"0"). Names are noise-stripped
    // first by [normalizeForTokens], so trailing flags/punctuation don't block the match.
    private fun tokenRegex(vararg tokens: String): Regex {
        val group = tokens.joinToString("|")
        return Regex("""(?<![0-9])($group)(?![a-z0-9])""", RegexOption.IGNORE_CASE)
    }

    private val allQualityTokens = Regex(
        """(?<![0-9])(4k|uhd|2160p?|2k|1440p?|qhd|fhd|full\s*hd|1080p?|hd|720p?|sd|480p?|360p?)(?![a-z0-9])""",
        RegexOption.IGNORE_CASE,
    )

    // Leading provider/country prefix: "ES|", "ES:", "EN -", "VIP >" and the pipe-wrapped form
    // "|IT| ", "|DE|  " that many M3U panels use. The optional leading separator catches the
    // wrapped form; a trailing separator is always required, so a plain word ("La Liga", "Al
    // Jazeera") is never mistaken for a prefix.
    private val leadingPrefix = Regex("""^\s*[|:>\-•]?\s*[\p{L}0-9]{1,4}\s*[|:>\-•]\s*""")
    private val bracketed = Regex("""[\[(][^\])]*[\])]""")
    // Strip emoji / flags / leftover symbol noise (keep letters, numbers, spaces, basic punctuation).
    private val noise = Regex("""[^\p{L}\p{N}\s&+./'-]""")
    private val multiSpace = Regex("""\s{2,}""")

    // Brand-agnostic on purpose: only generic sport terms, sport types and competitions — never
    // channel brands (DAZN, beIN, Movistar, Sky Sport…). A brand-only channel ("DAZN 1") is caught
    // by its category instead (see [isSports]). Keywords match at a word start (see [boundaryRegex]),
    // so "spor"/"sport" still covers brands that begin a word with it ("SporTV", "Sky Sport",
    // "Fox Sports") but not ones where it is glued mid-word ("Eurosport") — those rely on category.
    private val sportsKeywords = listOf(
        // Generic "sport" in many languages
        "sport", "deporte", "esporte", "esport", "spor", "رياضة", "رياضي", "اسبور",
        "спорт", "체육", "运动", "體育",
        // Football / soccer (multi-language)
        "futbol", "fútbol", "football", "soccer", "fußball", "fussball", "calcio", "futebol",
        "voetbal", "fudbal", "ποδόσφαιρο", "كرة",
        // Leagues & competitions
        "laliga", "la liga", "liga", "ligue", "premier league", "championship", "bundesliga",
        "eredivisie", "serie a", "serie b", "primeira", "champions", "uefa", "europa league",
        "conference league", "copa", "libertadores", "sudamericana", "fa cup", "efl", "mls",
        "super lig", "jupiler", "superliga", "eliteserien", "allsvenskan", "fifa",
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

    // Football by sport/competition terms only — no channel brands. A football channel named after
    // its competition still matches (LaLiga, Champions…); "DAZN LaLiga" matches via "laliga".
    private val footballKeywords = listOf(
        "futbol", "fútbol", "football", "soccer", "fußball", "fussball", "calcio", "futebol",
        "voetbal", "fudbal", "ποδόσφαιρο",
        "laliga", "la liga", "premier league", "championship", "bundesliga", "eredivisie",
        "serie a", "serie b", "primeira liga", "ligue 1", "ligue 2",
        "champions", "uefa", "europa league", "conference league", "copa del rey", "copa",
        "libertadores", "sudamericana", "fa cup", "efl", "mls", "super lig", "jupiler", "fifa",
    )

    // Trailing channel number (e.g. "beIN Sports 1" -> "beIN Sports"); keeps 4-digit+ tokens.
    private val trailingNumber = Regex("""[\s\-_]*\d{1,3}$""")

    /** Brand/family name: the base name with the trailing channel number removed. */
    fun folderName(baseName: String): String {
        val stripped = trailingNumber.replace(baseName, "").trim().trim('-', '_', '·').trim()
        return stripped.ifBlank { baseName }
    }

    fun quality(rawName: String): Quality {
        val name = normalizeForTokens(rawName)
        for ((quality, pattern) in qualityPatterns) {
            if (pattern.containsMatchIn(name)) return quality
        }
        return Quality.UNKNOWN
    }

    /** Canonical channel name with quality tags, prefixes and noise removed. */
    fun baseName(rawName: String): String {
        val name = allQualityTokens.replace(normalizeForTokens(rawName), " ")
        return multiSpace.replace(name, " ").trim().trim('-', '|', ':', '.', '·').trim()
    }

    /**
     * Strips brackets, a leading provider prefix and symbol/emoji noise — but NOT quality tags —
     * so quality detection runs on a clean string. Crucially this removes trailing flag emojis
     * before quality is read, so a glued tag like "LigaSmartBankHD🇪🇸" still resolves to "HD".
     */
    private fun normalizeForTokens(rawName: String): String {
        var name = bracketed.replace(rawName, " ")
        name = leadingPrefix.replace(name, "")
        name = noise.replace(name, " ")
        return multiSpace.replace(name, " ").trim()
    }

    /** Stable identity used to group quality variants of the same channel. */
    fun groupKey(rawName: String): String = baseName(rawName).lowercase()

    // Keywords match only at the start of a word (not preceded by a letter), so "liga" no longer
    // matches "alligator"/"hooligans"/"obligations"/"caligari" while still catching "LaLiga",
    // "Liga SmartBank", and prefixes like "spor" -> "Sport"/"SporTV".
    private val sportsRegex = boundaryRegex(sportsKeywords)
    private val footballRegex = boundaryRegex(footballKeywords)

    // Agnostic: a channel is sport if a sport term appears in its name or its category. No specific
    // channel names are listed anywhere — neither to include (brands) nor to exclude (broadcasters).
    fun isSports(channelName: String, categoryName: String?): Boolean =
        matchesAny(channelName, sportsRegex) || matchesAny(categoryName, sportsRegex)

    fun isFootball(channelName: String, categoryName: String?): Boolean =
        matchesAny(channelName, footballRegex) || matchesAny(categoryName, footballRegex)

    // "VOD" as a whole word in the category (so "VOD FR" matches but "VODAFONE" does not). Used to
    // drop video-on-demand that panels file under live categories (e.g. Xtream get_live_streams).
    private val vodCategory = Regex("""\bvod\b""", RegexOption.IGNORE_CASE)

    fun isVodCategory(category: String?): Boolean =
        category != null && vodCategory.containsMatchIn(category)

    private fun boundaryRegex(keywords: List<String>): Regex {
        val alternation = keywords.joinToString("|") { Regex.escape(it) }
        return Regex("""(?<!\p{L})($alternation)""", RegexOption.IGNORE_CASE)
    }

    private fun matchesAny(text: String?, regex: Regex): Boolean {
        if (text.isNullOrBlank()) return false
        return regex.containsMatchIn(text)
    }
}
