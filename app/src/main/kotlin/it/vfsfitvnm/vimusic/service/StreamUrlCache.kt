package it.vfsfitvnm.vimusic.service

/**
 * Small in-memory cache so playback can reuse a stream URL that was prepared
 * ahead of time, without waiting on the network inside the player thread.
 */
class StreamUrlCache<T>(private val maxSize: Int = 8) {
    private val map = object : LinkedHashMap<String, T>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, T>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(key: String): T? = map[key]

    @Synchronized
    fun put(key: String, value: T) {
        map[key] = value
    }
}
