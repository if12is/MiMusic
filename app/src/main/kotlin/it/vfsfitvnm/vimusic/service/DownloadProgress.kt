package it.vfsfitvnm.vimusic.service

object DownloadProgress {
    fun startPosition(cachedFromStart: Long, total: Long?): Long {
        if (total == null || total <= 0L) return 0L
        return cachedFromStart.coerceIn(0L, total)
    }

    fun percent(position: Long, total: Long?): Int {
        if (total == null || total <= 0L) return 0
        return ((position * 100L) / total).toInt().coerceIn(0, 100)
    }
}
