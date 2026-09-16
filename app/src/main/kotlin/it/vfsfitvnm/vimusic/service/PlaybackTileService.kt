package it.vfsfitvnm.vimusic.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class PlaybackTileService : TileService() {
    override fun onClick() {
        val playing = qsTile?.state == Tile.STATE_ACTIVE
        sendBroadcast(
            Intent(if (playing) "it.vfsfitvnm.vimusic.pause" else "it.vfsfitvnm.vimusic.play")
                .setPackage(packageName)
        )
        qsTile?.state = if (playing) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }

    override fun onStartListening() {
        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }
}
