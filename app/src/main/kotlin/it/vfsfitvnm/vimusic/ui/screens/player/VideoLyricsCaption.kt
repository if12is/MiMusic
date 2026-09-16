package it.vfsfitvnm.vimusic.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import it.vfsfitvnm.kugou.KuGou
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.models.Lyrics
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.SynchronizedLyrics
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.color
import it.vfsfitvnm.vimusic.utils.isBlank
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.resolveLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun VideoLyricsCaption(
    mediaId: String,
    mediaMetadata: MediaMetadata,
    durationProvider: () -> Long,
    ensureSongInserted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (_, typography) = LocalAppearance.current
    val player = LocalPlayerServiceBinder.current?.player ?: return
    var lyrics by remember(mediaId) { mutableStateOf<Lyrics?>(null) }

    LaunchedEffect(mediaId) {
        withContext(Dispatchers.IO) {
            Database.lyrics(mediaId).collect { stored ->
                if (stored.isBlank()) {
                    var duration = withContext(Dispatchers.Main) { durationProvider() }
                    while (duration == C.TIME_UNSET) {
                        delay(120)
                        duration = withContext(Dispatchers.Main) { durationProvider() }
                    }
                    val resolved = runCatching {
                        resolveLyrics(
                            mediaId = mediaId,
                            title = mediaMetadata.title?.toString(),
                            artist = mediaMetadata.artist?.toString(),
                            durationMs = duration
                        )
                    }.getOrNull() ?: return@collect
                    if (resolved.hasText) {
                        ensureSongInserted()
                        Database.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = resolved.fixed,
                                synced = resolved.synced
                            )
                        )
                    }
                } else {
                    lyrics = stored
                }
            }
        }
    }

    val synced = lyrics?.synced?.takeIf { it.isNotBlank() } ?: return
    val synchronizedLyrics = remember(synced) {
        SynchronizedLyrics(KuGou.Lyrics(synced).lines) {
            player.currentPosition + 50
        }
    }

    LaunchedEffect(synchronizedLyrics) {
        while (isActive) {
            delay(50)
            synchronizedLyrics.update()
        }
    }

    val line = synchronizedLyrics.currentLine?.text?.trim().orEmpty()
    if (line.isEmpty()) return

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                )
            )
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        BasicText(
            text = line,
            style = typography.s.medium.center.color(Color.White),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
