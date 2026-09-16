package it.vfsfitvnm.vimusic.utils

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import it.vfsfitvnm.vimusic.models.Song

data class DeviceTrack(
    val song: Song,
    val folder: String,
    val album: String?
)

fun Context.queryDeviceTracks(): List<DeviceTrack> {
    val tracks = mutableListOf<DeviceTrack>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projection = mutableListOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM
    )
    val relativePathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        projection += MediaStore.Audio.Media.RELATIVE_PATH
        MediaStore.Audio.Media.RELATIVE_PATH
    } else {
        @Suppress("DEPRECATION")
        projection += MediaStore.Audio.Media.DATA
        MediaStore.Audio.Media.DATA
    }

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    contentResolver.query(
        collection,
        projection.toTypedArray(),
        selection,
        null,
        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val pathColumn = cursor.getColumnIndexOrThrow(relativePathColumn)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(collection, id)
            val rawPath = cursor.getString(pathColumn).orEmpty()
            val folder = folderNameFromPath(rawPath)
            val album = cursor.getString(albumColumn)?.takeIf { it.isNotBlank() && it != "<unknown>" }
            tracks += DeviceTrack(
                song = Song(
                    id = "local:$id",
                    title = cursor.getString(titleColumn) ?: uri.lastPathSegment.orEmpty(),
                    artistsText = cursor.getString(artistColumn),
                    durationText = formatAsDuration(cursor.getLong(durationColumn)),
                    thumbnailUrl = uri.toString()
                ),
                folder = folder,
                album = album
            )
        }
    }

    return tracks
}

fun Context.queryDeviceSongs(): List<Song> = queryDeviceTracks().map { it.song }

fun Song.asLocalMediaItem(): MediaItem {
    val uri = thumbnailUrl?.toUri()
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setCustomCacheKey(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setExtras(bundleOf("durationText" to durationText, "isLocal" to true))
                .build()
        )
        .build()
}

fun durationTextToMillis(text: String?): Long {
    if (text.isNullOrBlank()) return Long.MAX_VALUE
    val parts = text.trim().split(':').mapNotNull { it.toLongOrNull() }
    return when (parts.size) {
        1 -> parts[0] * 1000
        2 -> (parts[0] * 60 + parts[1]) * 1000
        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
        else -> Long.MAX_VALUE
    }
}

fun playlistFolderOf(name: String): String {
    val index = name.indexOf(" / ")
    return if (index > 0) name.substring(0, index) else ""
}

fun playlistNameWithoutFolder(name: String): String {
    val index = name.indexOf(" / ")
    return if (index > 0) name.substring(index + 3) else name
}

private fun folderNameFromPath(path: String): String {
    val trimmed = path.trim().trimEnd('/')
    if (trimmed.isEmpty()) return ""
    val parent = trimmed.substringBeforeLast('/', missingDelimiterValue = "")
        .substringAfterLast('/')
        .ifBlank { trimmed.substringAfterLast('/') }
    return parent.ifBlank { "" }
}
