package it.vfsfitvnm.vimusic.service

/**
 * Audio and video bytes must never share a cache identity.
 * Comparing a suffix-stripped id with a stored stream key treats a video
 * request as the audio that was already cached.
 */
object StreamKeys {
    const val VIDEO_SUFFIX = "#video"

    fun of(videoId: String, wantsVideo: Boolean): String =
        if (wantsVideo) videoId + VIDEO_SUFFIX else videoId

    fun songId(cacheKey: String): String = cacheKey.removeSuffix(VIDEO_SUFFIX)

    fun sameStream(requestedKey: String, storedKey: String): Boolean = requestedKey == storedKey
}
