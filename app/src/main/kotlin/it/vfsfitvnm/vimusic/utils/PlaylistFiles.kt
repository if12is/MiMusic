package it.vfsfitvnm.vimusic.utils

import android.content.Context
import android.net.Uri
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.bodies.SearchBody
import it.vfsfitvnm.innertube.requests.searchPage
import it.vfsfitvnm.innertube.utils.from
import it.vfsfitvnm.vimusic.models.Song
import kotlinx.coroutines.delay

data class ImportedTrack(
    val title: String? = null,
    val artist: String? = null,
    val youtubeId: String? = null
)

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

fun songsToCsv(songs: List<Song>): String {
    return buildString {
        append('\uFEFF')
        appendLine("Title,Artist,YouTube URL,Source")
        songs.forEach { song ->
            val url = if (song.id.startsWith("local:")) {
                song.thumbnailUrl.orEmpty()
            } else {
                "https://music.youtube.com/watch?v=${song.id}"
            }
            appendLine(
                listOf(song.title, song.artistsText.orEmpty(), url, "MiMusic")
                    .joinToString(",") { csvEscape(it) }
            )
        }
    }
}

fun Context.writeTextToUri(uri: Uri, text: String) {
    contentResolver.openOutputStream(uri)?.use { output ->
        output.write(text.toByteArray(Charsets.UTF_8))
    }
}

fun Context.readTextFromUri(uri: Uri): String {
    return contentResolver.openInputStream(uri)?.use { input ->
        input.bufferedReader(Charsets.UTF_8).readText()
    }.orEmpty()
}

fun parseM3uVideoIds(text: String): List<String> {
    return parsePlaylistFile(text).mapNotNull { it.youtubeId }.distinct()
}

fun parsePlaylistFile(text: String): List<ImportedTrack> {
    val trimmed = text.trim().stripBom()
    if (trimmed.isEmpty()) return emptyList()

    val looksLikeM3u = trimmed.startsWith("#EXTM3U", ignoreCase = true) ||
        trimmed.lineSequence().any { it.trimStart().startsWith("#EXTINF", ignoreCase = true) }

    val tracks = if (looksLikeM3u) {
        parseM3u(trimmed)
    } else {
        parseCsvOrList(trimmed)
    }

    return tracks
        .map { it.copy(title = it.title?.trim()?.ifBlank { null }, artist = it.artist?.trim()?.ifBlank { null }) }
        .filter { !it.youtubeId.isNullOrBlank() || !it.title.isNullOrBlank() }
        .distinctBy { it.youtubeId ?: "${it.artist.orEmpty()}|${it.title.orEmpty()}".lowercase() }
}

suspend fun resolveImportedTracks(
    tracks: List<ImportedTrack>,
    limit: Int = 150
): List<Song> {
    val songs = mutableListOf<Song>()
    val seen = mutableSetOf<String>()

    for (track in tracks.take(limit)) {
        val youtubeId = track.youtubeId
        if (!youtubeId.isNullOrBlank()) {
            if (seen.add(youtubeId)) {
                songs += Song(
                    id = youtubeId,
                    title = track.title ?: youtubeId,
                    artistsText = track.artist,
                    durationText = null,
                    thumbnailUrl = null
                )
            }
            continue
        }

        val query = listOfNotNull(track.artist, track.title)
            .joinToString(" ")
            .trim()
        if (query.isEmpty()) continue

        val match = runCatching {
            Innertube.searchPage(
                body = SearchBody(query = query, params = Innertube.SearchFilter.Song.value),
                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
            )?.getOrNull()?.items?.firstOrNull()
        }.getOrNull()

        val song = match?.let { item ->
            runCatching {
                Song(
                    id = item.key,
                    title = item.info?.name ?: track.title ?: query,
                    artistsText = item.authors
                        ?.mapNotNull { it.name }
                        ?.joinToString("")
                        ?.ifBlank { track.artist },
                    durationText = item.durationText,
                    thumbnailUrl = item.thumbnail?.url
                )
            }.getOrNull()
        }

        if (song != null && seen.add(song.id)) {
            songs += song
        }
        delay(60)
    }

    return songs
}

private fun parseM3u(text: String): List<ImportedTrack> {
    val tracks = mutableListOf<ImportedTrack>()
    var pendingTitle: String? = null
    var pendingArtist: String? = null

    text.lineSequence().forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty()) return@forEach
        if (line.startsWith("#EXTINF", ignoreCase = true)) {
            val info = line.substringAfter(',', missingDelimiterValue = "").trim()
            val split = splitArtistTitle(info)
            pendingArtist = split.first
            pendingTitle = split.second
            return@forEach
        }
        if (line.startsWith("#")) return@forEach

        val youtubeId = extractYoutubeId(line)
        val artistTitle = splitArtistTitle(line.takeIf { youtubeId == null }.orEmpty())
        tracks += ImportedTrack(
            title = pendingTitle ?: artistTitle.second,
            artist = pendingArtist ?: artistTitle.first,
            youtubeId = youtubeId
        )
        pendingTitle = null
        pendingArtist = null
    }
    return tracks
}

private fun parseCsvOrList(text: String): List<ImportedTrack> {
    val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    if (lines.isEmpty()) return emptyList()

    val delimiter = detectDelimiter(lines.take(8).joinToString("\n"))
    val rows = lines.map { parseCsvLine(it, delimiter) }.filter { it.isNotEmpty() }
    if (rows.isEmpty()) return emptyList()

    val header = rows.first().map { normalizeHeader(it) }
    val hasHeader = header.any { isKnownHeader(it) }

    return if (hasHeader) {
        val titleIdx = indexOfHeader(
            header,
            "title", "track name", "track title", "song", "song name", "video title", "name",
            "عنوان", "أغنية", "اسم الأغنية", "اسم الاغنية"
        )
        val artistIdx = indexOfHeader(
            header,
            "artist", "artist name", "artists", "artist name(s)", "artists name", "singer",
            "فنان", "الفنان", "مطرب", "المطرب", "مغني"
        )
        val urlIdx = indexOfHeader(
            header,
            "url", "uri", "link", "track uri", "track url", "spotify", "youtube",
            "youtube url", "youtube id", "anghami", "video id", "id", "رابط"
        )
        rows.drop(1).mapNotNull { columns ->
            val url = columns.getOrNull(urlIdx).orEmpty()
            val title = columns.getOrNull(titleIdx)?.ifBlank { null }
            val artist = columns.getOrNull(artistIdx)?.ifBlank { null }
            val youtubeId = extractYoutubeId(url) ?: extractYoutubeId(columns.joinToString(" "))
            if (youtubeId == null && title == null) null
            else ImportedTrack(title = title, artist = artist, youtubeId = youtubeId)
        }
    } else {
        rows.mapNotNull { columns ->
            val joined = columns.joinToString(" ")
            val youtubeId = extractYoutubeId(joined)
            when {
                youtubeId != null -> ImportedTrack(
                    title = columns.getOrNull(0)?.takeIf { extractYoutubeId(it) == null },
                    artist = columns.getOrNull(1)?.takeIf { extractYoutubeId(it) == null },
                    youtubeId = youtubeId
                )
                columns.size >= 2 -> ImportedTrack(title = columns[0], artist = columns[1])
                else -> {
                    val split = splitArtistTitle(columns.firstOrNull().orEmpty())
                    if (split.second.isNullOrBlank()) null
                    else ImportedTrack(title = split.second, artist = split.first)
                }
            }
        }
    }
}

private fun detectDelimiter(sample: String): Char {
    val counts = mapOf(
        ',' to sample.count { it == ',' },
        ';' to sample.count { it == ';' },
        '\t' to sample.count { it == '\t' }
    )
    return counts.maxBy { it.value }.takeIf { it.value > 0 }?.key ?: ','
}

private fun parseCsvLine(line: String, delimiter: Char): List<String> {
    val out = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var index = 0
    while (index < line.length) {
        val char = line[index]
        when {
            char == '"' -> {
                if (inQuotes && index + 1 < line.length && line[index + 1] == '"') {
                    current.append('"')
                    index += 1
                } else {
                    inQuotes = !inQuotes
                }
            }
            char == delimiter && !inQuotes -> {
                out += current.toString().trim()
                current.clear()
            }
            else -> current.append(char)
        }
        index += 1
    }
    out += current.toString().trim()
    return out
}

private fun normalizeHeader(value: String): String {
    return value.lowercase()
        .replace('_', ' ')
        .replace('-', ' ')
        .replace("(", "")
        .replace(")", "")
        .trim()
}

private fun isKnownHeader(header: String): Boolean {
    val tokens = listOf(
        "title", "track", "song", "artist", "album", "url", "uri", "link",
        "spotify", "youtube", "anghami", "video id", "isrc",
        "عنوان", "أغنية", "فنان", "مطرب", "رابط"
    )
    return tokens.any { header == it || header.contains(it) }
}

private fun indexOfHeader(headers: List<String>, vararg names: String): Int {
    names.forEach { name ->
        val exact = headers.indexOfFirst { it == name }
        if (exact >= 0) return exact
    }
    names.forEach { name ->
        val partial = headers.indexOfFirst { it.contains(name) }
        if (partial >= 0) return partial
    }
    return -1
}

private fun extractYoutubeId(text: String): String? {
    if (text.isBlank()) return null
    Regex("[?&]v=([a-zA-Z0-9_-]{11})").find(text)?.groupValues?.getOrNull(1)?.let { return it }
    Regex("youtu\\.be/([a-zA-Z0-9_-]{11})").find(text)?.groupValues?.getOrNull(1)?.let { return it }
    Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})").find(text)?.groupValues?.getOrNull(1)?.let { return it }
    Regex("music\\.youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})").find(text)?.groupValues?.getOrNull(1)?.let { return it }
    if (text.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return text
    return null
}

private fun splitArtistTitle(value: String): Pair<String?, String?> {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null to null
    val separators = listOf(" - ", " – ", " — ", " | ")
    separators.forEach { separator ->
        val index = trimmed.indexOf(separator)
        if (index > 0) {
            return trimmed.substring(0, index).trim() to trimmed.substring(index + separator.length).trim()
        }
    }
    return null to trimmed
}

private fun csvEscape(value: String): String {
    return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }
}

private fun String.stripBom(): String = removePrefix("\uFEFF")
