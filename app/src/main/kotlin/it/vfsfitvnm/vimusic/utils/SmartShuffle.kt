package it.vfsfitvnm.vimusic.utils

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

fun List<MediaItem>.smartShuffled(): List<MediaItem> {
    if (size <= 2) return shuffled()

    val remaining = toMutableList().apply { shuffle() }
    val result = mutableListOf<MediaItem>()
    var lastArtist = ""

    while (remaining.isNotEmpty()) {
        val index = remaining.indexOfFirst { item ->
            val artist = item.mediaMetadata.artist?.toString().orEmpty()
            artist.isEmpty() || artist != lastArtist
        }.takeIf { it >= 0 } ?: 0

        val next = remaining.removeAt(index)
        result += next
        lastArtist = next.mediaMetadata.artist?.toString().orEmpty()
    }

    return result
}

fun Player.smartShuffleQueue() {
    val current = currentMediaItem ?: return
    val rest = currentTimeline.mediaItems.filterNot { it.mediaId == current.mediaId }.smartShuffled()
    if (currentMediaItemIndex > 0) removeMediaItems(0, currentMediaItemIndex)
    if (currentMediaItemIndex < mediaItemCount - 1) {
        removeMediaItems(currentMediaItemIndex + 1, mediaItemCount)
    }
    addMediaItems(rest)
}
