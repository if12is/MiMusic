package it.vfsfitvnm.vimusic

import it.vfsfitvnm.vimusic.utils.PlaybackPolicy
import it.vfsfitvnm.vimusic.utils.Region

/**
 * Shared decisions that are not a screen. [it.vfsfitvnm.vimusic.ui.screens.home.HomeViewModel]
 * and [it.vfsfitvnm.vimusic.ui.screens.player.PlayerSessionModel] come from Koin.
 */
object AppGraph {
    val region = Region

    fun choosePlayback(
        fullyDownloaded: Boolean,
        offlineOrNoNetwork: Boolean,
        hasPartialCache: Boolean
    ) = PlaybackPolicy.choose(fullyDownloaded, offlineOrNoNetwork, hasPartialCache)
}
