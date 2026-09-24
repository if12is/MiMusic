package it.vfsfitvnm.vimusic.utils

import it.vfsfitvnm.innertube.utils.ExtraTrack

fun quranSearchQuery(query: String): String {
    val trimmed = query.trim()
    if (trimmed.contains("قرآن") || trimmed.contains("quran", ignoreCase = true)) return trimmed
    return if (trimmed.isEmpty()) "قرآن" else "$trimmed قرآن"
}

fun podcastTracks(tracks: List<ExtraTrack>): List<ExtraTrack> =
    tracks.filter { it.source.equals("Podcast", ignoreCase = true) }

fun <T> chartsOrSearch(charts: List<T>?, search: List<T>?): List<T> =
    charts?.takeIf { it.isNotEmpty() } ?: search.orEmpty()

fun playbackLookahead(lowPower: Boolean): Int = if (lowPower) 1 else 3

fun artworkRequestSize(sizePx: Int, lowPower: Boolean): Int {
    if (!lowPower || sizePx <= 1) return sizePx
    return (sizePx / 2).coerceAtLeast(1)
}
