package it.vfsfitvnm.vimusic

import it.vfsfitvnm.vimusic.utils.PlaybackPolicy
import it.vfsfitvnm.vimusic.utils.Region

/**
 * Manual composition root. Screens use [androidx.lifecycle.viewmodel.compose.viewModel]
 * for [it.vfsfitvnm.vimusic.ui.screens.home.HomeViewModel] and
 * [it.vfsfitvnm.vimusic.ui.screens.player.PlayerSessionModel]. Shared decisions that
 * are not a screen live here so the player service and the home screen share one path.
 */
object AppGraph {
    val region = Region

    fun choosePlayback(
        fullyDownloaded: Boolean,
        offlineOrNoNetwork: Boolean,
        hasPartialCache: Boolean
    ) = PlaybackPolicy.choose(fullyDownloaded, offlineOrNoNetwork, hasPartialCache)
}
