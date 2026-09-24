package it.vfsfitvnm.vimusic.service

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import it.vfsfitvnm.innertube.requests.hasRealMusicVideo
import it.vfsfitvnm.innertube.utils.ExtraMediaIds
import it.vfsfitvnm.vimusic.utils.PlaybackLogStore
import java.io.IOException

/**
 * Plays a real music video when one exists, and the normal audio source otherwise.
 * Progressive itag 18/22 is one file. Separate video and audio tracks are merged
 * so the picture is not played without the song.
 */
@UnstableApi
internal class VideoOrAudioMediaSourceFactory(
    private val audio: MediaSource.Factory,
    private val direct: ProgressiveMediaSource.Factory,
    private val videoEnabled: () -> Boolean,
    private val offline: () -> Boolean,
    private val downloaded: (String) -> Boolean,
    private val resolve: (String) -> ResolvedStream,
    private val onVideo: (ResolvedStream) -> Unit
) : MediaSource.Factory {
    override fun setDrmSessionManagerProvider(
        drmSessionManagerProvider: DrmSessionManagerProvider
    ): MediaSource.Factory = apply {
        audio.setDrmSessionManagerProvider(drmSessionManagerProvider)
        direct.setDrmSessionManagerProvider(drmSessionManagerProvider)
    }

    override fun setLoadErrorHandlingPolicy(
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy
    ): MediaSource.Factory = apply {
        audio.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        direct.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
    }

    override fun getSupportedTypes(): IntArray = audio.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        if (!shouldPlayVideo(mediaItem)) return audio.createMediaSource(mediaItem)
        return try {
            buildVideo(mediaItem) ?: audio.createMediaSource(mediaItem)
        } catch (e: PlaybackException) {
            PlaybackLogStore.append("video source ${e.javaClass.simpleName}: ${e.message}")
            audio.createMediaSource(mediaItem)
        } catch (e: IOException) {
            PlaybackLogStore.append("video source ${e.javaClass.simpleName}: ${e.message}")
            audio.createMediaSource(mediaItem)
        }
    }

    private fun shouldPlayVideo(mediaItem: MediaItem): Boolean {
        val mediaId = mediaItem.mediaId
        if (mediaId.startsWith("local:") || ExtraMediaIds.isExternal(mediaId)) return false
        val cacheKey = mediaItem.localConfiguration?.customCacheKey
        val requested = cacheKey?.endsWith(StreamKeys.VIDEO_SUFFIX) == true || videoEnabled()
        if (!requested) return false
        if (offline() && downloaded(mediaId)) return false
        return true
    }

    private fun buildVideo(mediaItem: MediaItem): MediaSource? {
        val mediaId = mediaItem.mediaId
        val resolved = resolve(mediaId)
        if (!resolved.response.hasRealMusicVideo()) return null
        val choice = resolved.response.streamingData?.chooseVideo() ?: return null
        val videoUrl = choice.video.url ?: return null
        onVideo(resolved)
        val videoItem = mediaItem.buildUpon()
            .setUri(videoUrl)
            .setCustomCacheKey(StreamKeys.of(mediaId, true))
            .build()
        val videoSource = direct.createMediaSource(videoItem)
        val audioUrl = choice.audio?.url ?: return videoSource
        val audioItem = mediaItem.buildUpon()
            .setUri(audioUrl)
            .setCustomCacheKey(StreamKeys.of(mediaId, true) + "#audio")
            .build()
        return MergingMediaSource(
            /* adjustPeriodTimeOffsets = */ true,
            /* clipDurations = */ true,
            videoSource,
            direct.createMediaSource(audioItem)
        )
    }
}
