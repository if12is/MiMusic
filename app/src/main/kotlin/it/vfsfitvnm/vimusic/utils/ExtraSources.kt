package it.vfsfitvnm.vimusic.utils

import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import it.vfsfitvnm.innertube.utils.ExtraTrack
import it.vfsfitvnm.innertube.utils.searchSoundCloud
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

fun ExtraTrack.asMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(mediaId)
        .setCustomCacheKey(mediaId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist ?: source)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        "source" to source
                    )
                )
                .build()
        )
        .build()
}

suspend fun searchExtraSources(
    query: String,
    podcastFeeds: List<String>,
    preferences: android.content.SharedPreferences
): List<ExtraTrack> {
    if (query.isBlank()) return emptyList()
    return supervisorScope {
        val soundCloud = async(Dispatchers.IO) {
            runCatching { searchSoundCloud(query) }.getOrDefault(emptyList())
        }
        val podcasts = async(Dispatchers.IO) {
            runCatching { fetchPodcastEpisodes(podcastFeeds, query) }.getOrDefault(emptyList())
        }
        val jellyfin = async(Dispatchers.IO) {
            runCatching { Jellyfin.search(preferences, query) }.getOrDefault(emptyList())
        }
        (soundCloud.await() + podcasts.await() + jellyfin.await()).distinctBy { it.mediaId }
    }
}
