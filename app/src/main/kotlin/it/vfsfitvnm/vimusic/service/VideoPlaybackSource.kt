package it.vfsfitvnm.vimusic.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.CompositeMediaSource
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import it.vfsfitvnm.innertube.requests.hasRealMusicVideo
import it.vfsfitvnm.innertube.utils.ExtraMediaIds
import it.vfsfitvnm.vimusic.utils.PlaybackLogStore
import kotlin.coroutines.cancellation.CancellationException

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
        // ExoPlayer calls this on the thread that tapped play. Resolving the stream
        // here crashed the app (IllegalStateException: must not block the main thread)
        // and, once video mode was saved, every following song crashed the same way.
        return SameFileVideoSource(
            mediaItem = mediaItem,
            fallback = { audio.createMediaSource(mediaItem) },
            video = { buildVideo(mediaItem) }
        )
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

/**
 * Resolves the picture on the playback thread, then plays that same file.
 * The tap that starts a song stays on the main thread and must not do network work.
 * If the picture cannot be opened, the song's sound still plays.
 */
@UnstableApi
private class SameFileVideoSource(
    private val mediaItem: MediaItem,
    private val fallback: () -> MediaSource,
    private val video: () -> MediaSource?
) : CompositeMediaSource<Int>() {
    private var child: MediaSource? = null

    override fun getMediaItem(): MediaItem = mediaItem

    @Suppress("TooGenericExceptionCaught")
    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        super.prepareSourceInternal(mediaTransferListener)
        val source = try {
            video() ?: fallback()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            PlaybackLogStore.append("video source ${error.javaClass.simpleName}: ${error.message}")
            fallback()
        }
        child = source
        prepareChildSource(CHILD, source)
    }

    override fun onChildSourceInfoRefreshed(
        childSourceId: Int,
        mediaSource: MediaSource,
        newTimeline: Timeline
    ) {
        refreshSourceInfo(newTimeline)
    }

    override fun createPeriod(
        id: MediaSource.MediaPeriodId,
        allocator: Allocator,
        startPositionUs: Long
    ): MediaPeriod {
        return checkNotNull(child).createPeriod(id, allocator, startPositionUs)
    }

    override fun releasePeriod(mediaPeriod: MediaPeriod) {
        child?.releasePeriod(mediaPeriod)
    }

    private companion object {
        const val CHILD = 0
    }
}
