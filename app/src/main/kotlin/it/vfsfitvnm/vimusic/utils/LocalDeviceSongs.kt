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

fun Context.queryDeviceSongs(): List<Song> {
    val songs = mutableListOf<Song>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    contentResolver.query(
        collection,
        projection,
        selection,
        null,
        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(collection, id)
            songs += Song(
                id = "local:$id",
                title = cursor.getString(titleColumn) ?: uri.lastPathSegment.orEmpty(),
                artistsText = cursor.getString(artistColumn),
                durationText = formatAsDuration(cursor.getLong(durationColumn)),
                thumbnailUrl = uri.toString()
            )
        }
    }

    return songs
}

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
