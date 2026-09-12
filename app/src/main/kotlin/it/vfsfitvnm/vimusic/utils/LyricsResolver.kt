package it.vfsfitvnm.vimusic.utils

import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.bodies.NextBody
import it.vfsfitvnm.innertube.requests.lyrics
import it.vfsfitvnm.kugou.KuGou
import it.vfsfitvnm.kugou.LrcLib
import it.vfsfitvnm.kugou.LrcParser
import it.vfsfitvnm.vimusic.models.Lyrics

internal data class ResolvedLyrics(
    val fixed: String?,
    val synced: String?
) {
    val hasText: Boolean
        get() = !fixed.isNullOrBlank() || !synced.isNullOrBlank()
}

internal suspend fun resolveLyrics(
    mediaId: String,
    title: String?,
    artist: String?,
    durationMs: Long
): ResolvedLyrics {
    val cleanedTitle = title?.trim().orEmpty()
    val cleanedArtist = artist?.trim()
        ?.takeUnless { it.equals("null", ignoreCase = true) }
        .orEmpty()
    val durationSeconds = (durationMs / 1000).coerceAtLeast(0)

    var fixed = runCatching {
        Innertube.lyrics(NextBody(videoId = mediaId))?.getOrNull()
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    var synced: String? = null

    val lrcLib = runCatching {
        LrcLib.lyrics(cleanedArtist, cleanedTitle, durationSeconds)?.getOrNull()
    }.getOrNull()
    if (fixed.isNullOrBlank()) {
        fixed = lrcLib?.plain?.trim()?.takeIf { it.isNotEmpty() }
    }
    synced = lrcLib?.synced?.trim()?.takeIf { it.isNotEmpty() }

    if (synced.isNullOrBlank()) {
        synced = runCatching {
            KuGou.lyrics(
                artist = cleanedArtist,
                title = cleanedTitle,
                duration = durationSeconds
            )?.getOrNull()?.value
        }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    if (fixed.isNullOrBlank() && !synced.isNullOrBlank()) {
        fixed = LrcParser.plainText(synced).takeIf { it.isNotBlank() }
    }

    return ResolvedLyrics(fixed = fixed, synced = synced)
}

internal fun Lyrics?.isBlank(): Boolean =
    this == null || (fixed.isNullOrBlank() && synced.isNullOrBlank())
