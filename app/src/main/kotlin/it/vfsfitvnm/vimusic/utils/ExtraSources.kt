package it.vfsfitvnm.vimusic.utils

import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import it.vfsfitvnm.innertube.utils.ExtraTrack
import it.vfsfitvnm.innertube.utils.searchSoundCloud

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

fun searchExtraSources(
    query: String,
    podcastFeeds: List<String>,
    preferences: android.content.SharedPreferences
): List<ExtraTrack> {
    val soundCloud = runCatching { searchSoundCloud(query) }.getOrDefault(emptyList())
    val podcasts = runCatching { fetchPodcastEpisodes(podcastFeeds, query) }.getOrDefault(emptyList())
    val jellyfin = runCatching { Jellyfin.search(preferences, query) }.getOrDefault(emptyList())
    return (soundCloud + podcasts + jellyfin).distinctBy { it.mediaId }
}
