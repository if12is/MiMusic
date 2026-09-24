package it.vfsfitvnm.vimusic.service

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.models.SongWithContentLength
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.forceSeekToNext
import it.vfsfitvnm.vimusic.utils.forceSeekToPrevious
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@UnstableApi
class SkipAwarePlayer(private val base: Player) : ForwardingPlayer(base) {
    override fun seekToNext() = base.forceSeekToNext()
    override fun seekToPrevious() = base.forceSeekToPrevious()
}

@UnstableApi
class LibrarySessionCallback(
    private val isDownloaded: (SongWithContentLength) -> Boolean
) : MediaSession.Callback {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val requested = mediaItems.singleOrNull()?.mediaId
        if (requested == null || !LibraryQueue.isLibraryId(requested)) {
            return Futures.immediateFuture(mediaItems)
        }
        val future = SettableFuture.create<MutableList<MediaItem>>()
        scope.launch {
            val resolved = runCatching { load(requested) }.getOrDefault(emptyList())
            if (resolved.isEmpty()) future.set(mediaItems) else future.set(resolved.toMutableList())
        }
        return future
    }

    private suspend fun load(mediaId: String): List<MediaItem> {
        val kind = mediaId.substringBefore('/')
        val argument = LibraryQueue.argument(mediaId)
        val songs = when (kind) {
            "songs", "shuffle" -> Database.songsByPlayTimeDesc().first().take(30)
            "favorites" -> Database.favorites().first()
            "history" -> Database.playbackHistory().first()
            "quran" -> Database.quranSongs().first()
            "offline" -> Database.downloadedSongs().first()
                .filter(isDownloaded)
                .map { it.song }
            "playlists" -> argument?.toLongOrNull()
                ?.let(Database::playlistWithSongs)
                ?.first()
                ?.songs
                .orEmpty()
            "albums" -> argument?.let(Database::albumSongs)?.first().orEmpty()
            else -> emptyList()
        }
        val ids = LibraryQueue.fromSelection(songs.map { it.id }, argument)
        val selected = songs.filter { it.id in ids.toSet() }
        val ordered = ids.mapNotNull { id -> selected.find { it.id == id } }
        return ordered.map { it.asMediaItem }
    }
}
