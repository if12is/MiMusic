package it.vfsfitvnm.vimusic.ui.screens.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import it.vfsfitvnm.vimusic.utils.shouldBePlaying

class PlayerSessionModel : ViewModel() {
    var mediaItem by mutableStateOf<MediaItem?>(null, neverEqualPolicy())
        private set
    var shouldBePlaying by mutableStateOf(false)
        private set

    private var player: Player? = null
    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            this@PlayerSessionModel.mediaItem = mediaItem
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            shouldBePlaying = player?.shouldBePlaying == true
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            shouldBePlaying = player?.shouldBePlaying == true
        }
    }

    fun attach(player: Player) {
        if (this.player === player) {
            mediaItem = player.currentMediaItem
            shouldBePlaying = player.shouldBePlaying
            return
        }
        detach()
        this.player = player
        mediaItem = player.currentMediaItem
        shouldBePlaying = player.shouldBePlaying
        player.addListener(listener)
    }

    private fun detach() {
        player?.removeListener(listener)
        player = null
    }

    override fun onCleared() {
        detach()
    }
}
