package it.vfsfitvnm.vimusic.utils

import it.vfsfitvnm.vimusic.models.SongWithContentLength
import java.util.Locale

enum class DownloadSort {
    Recent,
    Title,
    Size
}

fun List<SongWithContentLength>.sortedDownloads(sort: DownloadSort): List<SongWithContentLength> =
    when (sort) {
        DownloadSort.Recent -> this
        DownloadSort.Title -> sortedBy { it.song.title.lowercase(Locale.ROOT) }
        DownloadSort.Size -> sortedByDescending { it.contentLength ?: 0L }
    }

fun totalDownloadBytes(songs: List<SongWithContentLength>): Long =
    songs.sumOf { it.contentLength ?: 0L }

fun formatByteSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit += 1
    }
    return if (unit == 0) {
        "$bytes B"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[unit])
    }
}
