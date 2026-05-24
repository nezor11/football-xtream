package com.footballxtream.data

import com.footballxtream.model.LiveChannel

/** Parses an M3U / M3U-plus playlist into [LiveChannel]s. */
object M3uParser {

    private val tvgName = Regex("""tvg-name="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val groupTitle = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val tvgLogo = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)

    fun parse(content: String): List<LiveChannel> {
        val channels = ArrayList<LiveChannel>()
        var name: String? = null
        var group: String? = null
        var logo: String? = null

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val attrName = tvgName.find(line)?.groupValues?.get(1)?.trim().orEmpty()
                    val trailingName = line.substringAfterLast(',', "").trim()
                    name = attrName.ifBlank { trailingName }
                    group = groupTitle.find(line)?.groupValues?.get(1)?.trim()
                    logo = tvgLogo.find(line)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
                }

                line.isEmpty() || line.startsWith("#") -> Unit // skip other directives

                else -> {
                    // URL line that closes the current #EXTINF entry. Keep only live channels
                    // (Xtream VOD/series URLs carry /movie/ or /series/), which also speeds up
                    // parsing a huge playlist and avoids films matching sports keywords.
                    val channelName = name
                    val isLive = !line.contains("/movie/", true) && !line.contains("/series/", true)
                    if (isLive && channelName != null && channelName.isNotBlank()) {
                        channels += LiveChannel(
                            streamId = line.hashCode(),
                            name = channelName,
                            iconUrl = logo,
                            categoryName = group,
                            streamUrl = line,
                        )
                    }
                    name = null
                    group = null
                    logo = null
                }
            }
        }
        return channels
    }
}
