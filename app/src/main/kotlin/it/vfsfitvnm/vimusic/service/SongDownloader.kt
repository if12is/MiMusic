package it.vfsfitvnm.vimusic.service

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.requests.player
import it.vfsfitvnm.innertube.models.PlayerResponse
import it.vfsfitvnm.innertube.models.bodies.PlayerBody
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.enums.AudioQuality
import it.vfsfitvnm.vimusic.models.Format
import it.vfsfitvnm.vimusic.models.Lyrics
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.innertube.utils.ExtraMediaIds
import it.vfsfitvnm.vimusic.utils.audioQualityKey
import it.vfsfitvnm.vimusic.utils.formatFor
import it.vfsfitvnm.vimusic.utils.getEnum
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.resolveLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@UnstableApi
object SongDownloader {
    const val CHUNK_LENGTH = 1024 * 1024L

    suspend fun download(
        context: Context,
        cache: Cache,
        mediaId: String,
        title: String?,
        artist: String?,
        album: String?,
        thumbnail: String?,
        onProgress: (Int) -> Unit
    ) {
        if (mediaId.startsWith("local:") || ExtraMediaIds.isExternal(mediaId)) {
            throw UnplayableException()
        }
        val (response, format) = resolveAudio(context, mediaId)
        val url = format.url?.toUri() ?: throw PlayableFormatNotFoundException()
        val totalLength = format.contentLength?.takeIf { it > 0 }
        val cachedFromStart = if (totalLength != null) {
            cache.getCachedLength(mediaId, 0, totalLength).coerceAtLeast(0L)
        } else {
            0L
        }
        val start = DownloadProgress.startPosition(cachedFromStart, totalLength)
        if (start == 0L) {
            cache.removeResource(mediaId)
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(mediaId)
            .setCustomCacheKey(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build()
            )
            .build()

        query {
            Database.insert(mediaItem)
            Database.insert(formatRow(mediaId, response, format, format.contentLength))
        }

        val writer = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory())
            .createDataSource()

        if (totalLength == null) {
            onProgress(0)
            CacheWriter(
                writer,
                DataSpec.Builder().setUri(url).setKey(mediaId).build(),
                null,
                null
            ).cache()
        } else {
            var position = start
            while (position < totalLength) {
                coroutineContext.ensureActive()
                val length = minOf(CHUNK_LENGTH, totalLength - position)
                CacheWriter(
                    writer,
                    DataSpec.Builder()
                        .setUri(url)
                        .setKey(mediaId)
                        .setPosition(position)
                        .setLength(length)
                        .build(),
                    null,
                    null
                ).cache()
                position += length
                onProgress(DownloadProgress.percent(position, totalLength))
            }
        }

        val storedLength = storedLength(cache, mediaId) ?: throw PlayableFormatNotFoundException()
        cache.applyContentMetadataMutations(
            mediaId,
            ContentMetadataMutations.setContentLength(ContentMetadataMutations(), storedLength)
        )
        query {
            Database.insert(formatRow(mediaId, response, format, storedLength))
        }
        saveLyrics(mediaItem, format.approxDurationMs ?: 0L)
        onProgress(100)
    }

    private suspend fun resolveAudio(
        context: Context,
        videoId: String
    ): Pair<PlayerResponse, PlayerResponse.StreamingData.AdaptiveFormat> {
        val response = withContext(Dispatchers.IO) {
            Innertube.player(PlayerBody(videoId = videoId))?.getOrThrow()
        } ?: throw PlayableFormatNotFoundException()
        when (response.playabilityStatus?.status) {
            "OK" -> Unit
            "LOGIN_REQUIRED" -> throw LoginRequiredException()
            "UNPLAYABLE" -> throw UnplayableException()
            else -> throw PlayableFormatNotFoundException()
        }
        val format = response.streamingData?.formatFor(
            context.preferences.getEnum(audioQualityKey, AudioQuality.Auto),
            preferMuxed = false
        ) ?: throw PlayableFormatNotFoundException()
        if (format.url.isNullOrBlank()) throw PlayableFormatNotFoundException()
        return response to format
    }

    private fun formatRow(
        mediaId: String,
        response: PlayerResponse,
        format: PlayerResponse.StreamingData.AdaptiveFormat,
        contentLength: Long?
    ) = Format(
        songId = mediaId,
        itag = format.itag,
        mimeType = format.mimeType,
        bitrate = format.bitrate,
        loudnessDb = response.playerConfig?.audioConfig?.normalizedLoudnessDb,
        contentLength = contentLength,
        lastModified = format.lastModified
    )

    private fun storedLength(cache: Cache, mediaId: String): Long? {
        val metadataLength = ContentMetadata.getContentLength(cache.getContentMetadata(mediaId))
        if (metadataLength != C.LENGTH_UNSET.toLong()) return metadataLength
        val cachedBytes = cache.getCachedBytes(mediaId, 0, Long.MAX_VALUE)
        return cachedBytes.takeIf { it > 0L }
    }

    private suspend fun saveLyrics(mediaItem: MediaItem, durationMs: Long) {
        runCatching {
            val stored = Database.lyrics(mediaItem.mediaId).firstOrNull()
            if (!stored?.fixed.isNullOrBlank() || !stored?.synced.isNullOrBlank()) return
            val resolved = resolveLyrics(
                mediaId = mediaItem.mediaId,
                title = mediaItem.mediaMetadata.title?.toString(),
                artist = mediaItem.mediaMetadata.artist?.toString(),
                durationMs = durationMs
            )
            if (resolved.hasText) {
                Database.upsert(
                    Lyrics(
                        songId = mediaItem.mediaId,
                        fixed = resolved.fixed,
                        synced = resolved.synced
                    )
                )
            }
        }
    }

    private fun httpFactory() = DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(16_000)
        .setReadTimeoutMs(20_000)
        .setUserAgent("com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip")
}
