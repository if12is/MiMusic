package it.vfsfitvnm.vimusic.utils

import android.content.Context
import android.net.Uri
import it.vfsfitvnm.vimusic.models.Song

fun songsToM3u(songs: List<Song>): String {
    return buildString {
        appendLine("#EXTM3U")
        songs.forEach { song ->
            appendLine("#EXTINF:-1,${song.artistsText.orEmpty()} - ${song.title}")
            if (song.id.startsWith("local:")) {
                appendLine(song.thumbnailUrl.orEmpty())
            } else {
                appendLine("https://music.youtube.com/watch?v=${song.id}")
            }
        }
    }
}

fun Context.writeTextToUri(uri: Uri, text: String) {
    contentResolver.openOutputStream(uri)?.use { output ->
        output.write(text.toByteArray())
    }
}

fun Context.readTextFromUri(uri: Uri): String {
    return contentResolver.openInputStream(uri)?.use { input ->
        input.bufferedReader().readText()
    }.orEmpty()
}

fun parseM3uVideoIds(text: String): List<String> {
    val ids = mutableListOf<String>()
    val watch = Regex("[?&]v=([a-zA-Z0-9_-]{11})")
    val short = Regex("youtu\\.be/([a-zA-Z0-9_-]{11})")
    text.lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("#") || trimmed.isEmpty()) return@forEach
        watch.find(trimmed)?.groupValues?.getOrNull(1)?.let(ids::add)
            ?: short.find(trimmed)?.groupValues?.getOrNull(1)?.let(ids::add)
    }
    return ids.distinct()
}
