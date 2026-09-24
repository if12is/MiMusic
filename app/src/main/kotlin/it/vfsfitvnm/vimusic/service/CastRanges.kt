package it.vfsfitvnm.vimusic.service

internal object CastRanges {
    fun bounds(header: String?, length: Long): LongRange {
        if (length <= 0L) return 0L..0L
        val last = length - 1
        if (header.isNullOrBlank() || !header.startsWith("bytes=")) return 0L..last
        val spec = header.removePrefix("bytes=").substringBefore(",")
        val startText = spec.substringBefore("-")
        val endText = spec.substringAfter("-", "")
        val start = (startText.toLongOrNull() ?: 0L).coerceIn(0L, last)
        val end = (endText.toLongOrNull() ?: last).coerceIn(start, last)
        return start..end
    }
}
