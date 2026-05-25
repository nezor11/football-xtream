package com.footballxtream.data

import com.footballxtream.model.LiveChannel

/** Parses an M3U / M3U-plus playlist into [LiveChannel]s. */
object M3uParser {

    private val tvgName = Regex("""tvg-name="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val tvgId = Regex("""tvg-id="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val groupTitle = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val tvgLogo = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)
    // EPG source(s) declared on the #EXTM3U header: x-tvg-url / url-tvg / tvg-url.
    private val epgUrlAttr = Regex("""(?:x-tvg-url|url-tvg|tvg-url)="([^"]*)"""", RegexOption.IGNORE_CASE)

    // Movie/series files; live streams are .ts/.m3u8 or extension-less. Some panels put the real
    // extension in a query value (…&v=film.mp4), so we test the whole URL, not just the path.
    private val vodExtensions = listOf(".mp4", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".m4v")
    // "VOD" as a whole word in the group title (so "VOD FR" matches but "VODAFONE" does not).
    private val vodGroup = Regex("""\bvod\b""", RegexOption.IGNORE_CASE)

    fun parse(content: String): List<LiveChannel> {
        val channels = ArrayList<LiveChannel>()
        var name: String? = null
        var group: String? = null
        var logo: String? = null
        var epgId: String? = null

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val attrName = tvgName.find(line)?.groupValues?.get(1)?.trim().orEmpty()
                    val trailingName = line.substringAfterLast(',', "").trim()
                    name = attrName.ifBlank { trailingName }
                    group = groupTitle.find(line)?.groupValues?.get(1)?.trim()
                    logo = tvgLogo.find(line)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
                    epgId = tvgId.find(line)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
                }

                line.isEmpty() || line.startsWith("#") -> Unit // skip other directives

                else -> {
                    // URL line that closes the current #EXTINF entry. Keep only live channels and
                    // drop video-on-demand (tens of thousands of films/series here), which also
                    // speeds up parsing and stops movies from matching sports keywords.
                    val channelName = name
                    val isLive = !isVod(line, group)
                    if (isLive && channelName != null && channelName.isNotBlank()) {
                        channels += LiveChannel(
                            streamId = line.hashCode(),
                            name = channelName,
                            iconUrl = logo,
                            categoryName = group,
                            streamUrl = line,
                            epgId = epgId,
                        )
                    }
                    name = null
                    group = null
                    logo = null
                    epgId = null
                }
            }
        }
        return channels
    }

    /**
     * EPG (XMLTV) source URLs declared on the `#EXTM3U` header line, if any. A playlist may list
     * several comma-separated URLs; each may be plain XML or gzipped (`.xml.gz`).
     */
    fun epgUrls(content: String): List<String> {
        val header = content.lineSequence().firstOrNull { it.trim().startsWith("#EXTM3U", true) }
            ?: return emptyList()
        val raw = epgUrlAttr.find(header)?.groupValues?.get(1) ?: return emptyList()
        return raw.split(',', ' ')
            .map { it.trim() }
            .filter { it.startsWith("http://", true) || it.startsWith("https://", true) }
    }

    /** True for video-on-demand entries (films/series), which must not appear as live channels. */
    private fun isVod(url: String, group: String?): Boolean {
        val u = url.lowercase()
        if (u.contains("/movie/") || u.contains("/series/") || u.contains("://vod.")) return true
        if (vodExtensions.any { u.endsWith(it) }) return true
        if (group != null && vodGroup.containsMatchIn(group)) return true
        return false
    }
}
