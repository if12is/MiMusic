package it.vfsfitvnm.kugou

data class LyricWord(
    val timeMs: Long,
    val text: String
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

object LrcParser {
    private val lineTimeRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val wordTimeRegex = Regex("""<(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?>""")

    fun parse(value: String): List<LyricLine> {
        val lines = mutableListOf(LyricLine(0L, ""))

        value.replace("\r\n", "\n").trim().lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || isMetadata(line)) return@forEach

            val stamps = lineTimeRegex.findAll(line).toList()
            if (stamps.isEmpty()) return@forEach

            val content = line.substring(stamps.last().range.last + 1).trim()
            val words = parseWords(content)
            val text = words.joinToString(" ") { it.text }.ifBlank { stripWordTags(content) }
            if (text.isBlank()) return@forEach

            stamps.forEach { match ->
                lines += LyricLine(
                    timeMs = timestampMs(match.groupValues),
                    text = text,
                    words = words
                )
            }
        }

        return lines.sortedBy(LyricLine::timeMs)
    }

    fun plainText(value: String): String =
        parse(value).asSequence()
            .map(LyricLine::text)
            .filter(String::isNotBlank)
            .joinToString("\n")

    private fun parseWords(content: String): List<LyricWord> {
        val matches = wordTimeRegex.findAll(content).toList()
        if (matches.isEmpty()) return emptyList()

        return matches.mapIndexed { index, match ->
            val start = match.range.last + 1
            val end = matches.getOrNull(index + 1)?.range?.first ?: content.length
            LyricWord(
                timeMs = timestampMs(match.groupValues),
                text = content.substring(start, end).trim()
            )
        }.filter { it.text.isNotEmpty() }
    }

    private fun stripWordTags(content: String): String =
        content.replace(wordTimeRegex, "").trim()

    private fun timestampMs(groups: List<String>): Long {
        val minutes = groups[1].toLong()
        val seconds = groups[2].toLong()
        val fraction = groups.getOrNull(3).orEmpty()
        val millis = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100
            2 -> fraction.toLong() * 10
            else -> fraction.take(3).toLong()
        }
        return minutes * 60_000 + seconds * 1_000 + millis
    }

    private fun isMetadata(line: String): Boolean =
        line.startsWith("[ti:") ||
            line.startsWith("[ar:") ||
            line.startsWith("[al:") ||
            line.startsWith("[by:") ||
            line.startsWith("[hash:") ||
            line.startsWith("[sign:") ||
            line.startsWith("[qq:") ||
            line.startsWith("[total:") ||
            line.startsWith("[offset:") ||
            line.startsWith("[id:") ||
            line.startsWith("[length:")
}
