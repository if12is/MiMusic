package it.vfsfitvnm.vimusic.service

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import it.vfsfitvnm.innertube.models.PlayerResponse
import it.vfsfitvnm.innertube.models.bodies.PlayerBody
import it.vfsfitvnm.vimusic.enums.AudioQuality
import it.vfsfitvnm.vimusic.utils.PlaybackLogStore
import it.vfsfitvnm.vimusic.utils.formatFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.os.Looper
import androidx.media3.common.util.UnstableApi

data class ResolvedStream(
    val key: String,
    val videoId: String,
    val uri: Uri,
    val progressiveMuxed: Boolean,
    val response: PlayerResponse,
    val format: PlayerResponse.StreamingData.AdaptiveFormat
)

fun PlayerResponse.hasMusicVideo(): Boolean =
    streamingData?.muxedFallbackFormat != null &&
        videoDetails?.musicVideoType != "MUSIC_VIDEO_TYPE_ATV"

@UnstableApi
class StreamResolver(
    private val cache: StreamUrlCache<ResolvedStream>,
    private val quality: () -> AudioQuality,
    private val player: suspend (PlayerBody, Boolean) -> Result<PlayerResponse>?
) {
    private val gates = java.util.concurrent.ConcurrentHashMap<String, Mutex>()

    fun peek(key: String): ResolvedStream? = cache.get(key)

    fun key(videoId: String, wantsVideo: Boolean): String = StreamKeys.of(videoId, wantsVideo)

    suspend fun resolve(videoId: String, wantsVideo: Boolean): ResolvedStream {
        val streamKey = key(videoId, wantsVideo)
        val mutex = gates.getOrPut(streamKey) { Mutex() }
        return mutex.withLock {
            cache.get(streamKey)?.let { return@withLock it }
            val resolved = fetch(videoId, wantsVideo, streamKey)
            cache.put(streamKey, resolved)
            resolved
        }
    }

    fun resolveBlocking(videoId: String, wantsVideo: Boolean): ResolvedStream {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Stream resolve must not block the main thread"
        }
        return runBlocking(Dispatchers.IO) { resolve(videoId, wantsVideo) }
    }

    fun prefetch(
        scope: CoroutineScope,
        videoIds: List<String>,
        wantsVideo: Boolean,
        onResolved: (ResolvedStream) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            videoIds.forEach { videoId ->
                if (videoId.startsWith("local:") || videoId.contains(":")) return@forEach
                if (peek(key(videoId, wantsVideo)) != null) return@forEach
                runCatching { resolve(videoId, wantsVideo) }.onSuccess(onResolved)
            }
        }
    }

    private suspend fun fetch(videoId: String, wantsVideo: Boolean, streamKey: String): ResolvedStream {
        PlaybackLogStore.append("player request $videoId")
        val body = player(PlayerBody(videoId = videoId), wantsVideo)?.getOrThrow()
            ?: throw PlayableFormatNotFoundException()
        val returnedVideoId = body.videoDetails?.videoId
        if (returnedVideoId != null && returnedVideoId != videoId) {
            throw VideoIdMismatchException()
        }
        when (val status = body.playabilityStatus?.status) {
            "OK" -> Unit
            "UNPLAYABLE" -> throw UnplayableException()
            "LOGIN_REQUIRED" -> throw LoginRequiredException()
            else -> throw PlaybackException(
                status,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }
        val format = body.streamingData?.formatFor(
            quality(),
            preferMuxed = wantsVideo && body.hasMusicVideo()
        ) ?: throw PlayableFormatNotFoundException()
        val streamUrl = format.url ?: throw PlayableFormatNotFoundException()
        val progressive = !format.isAudioOnly ||
            format.itag == 18 ||
            format.itag == 22 ||
            format.mimeType.contains("video", ignoreCase = true)
        PlaybackLogStore.append("resolved $videoId itag=${format.itag} mime=${format.mimeType}")
        return ResolvedStream(
            key = streamKey,
            videoId = videoId,
            uri = streamUrl.toUri(),
            progressiveMuxed = progressive,
            response = body,
            format = format
        )
    }

    companion object {
        const val VIDEO_KEY_SUFFIX = StreamKeys.VIDEO_SUFFIX
    }
}
