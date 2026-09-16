package it.vfsfitvnm.vimusic.ui.screens.builtinplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import it.vfsfitvnm.compose.persist.PersistMapCleanup
import it.vfsfitvnm.compose.routing.RouteHandler
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.BuiltInPlaylist
import it.vfsfitvnm.vimusic.ui.components.themed.Scaffold
import it.vfsfitvnm.vimusic.ui.screens.globalRoutes
import it.vfsfitvnm.vimusic.utils.LocalStrings

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun BuiltInPlaylistScreen(builtInPlaylist: BuiltInPlaylist) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val strings = LocalStrings.current

    val (tabIndex, onTabIndexChanged) = rememberSaveable {
        mutableStateOf(
            when (builtInPlaylist) {
                BuiltInPlaylist.Favorites -> 0
                BuiltInPlaylist.Offline -> 1
                BuiltInPlaylist.History -> 2
                BuiltInPlaylist.Top -> 3
                BuiltInPlaylist.Device -> 4
                BuiltInPlaylist.ThisWeek -> 5
                BuiltInPlaylist.ShortFavorites -> 6
            }
        )
    }

    PersistMapCleanup(tagPrefix = "${builtInPlaylist.name}/")

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()

        host {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = onTabIndexChanged,
                tabColumnContent = { Item ->
                    Item(0, strings.favorites, R.drawable.heart)
                    Item(1, strings.offline, R.drawable.airplane)
                    Item(2, strings.playbackHistory, R.drawable.time)
                    Item(3, strings.mostPlayed, R.drawable.trending)
                    Item(4, strings.onDevice, R.drawable.musical_notes)
                    Item(5, strings.thisWeek, R.drawable.calendar)
                    Item(6, strings.shortFavorites, R.drawable.heart_outline)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    BuiltInPlaylistSongs(
                        builtInPlaylist = when (currentTabIndex) {
                            1 -> BuiltInPlaylist.Offline
                            2 -> BuiltInPlaylist.History
                            3 -> BuiltInPlaylist.Top
                            4 -> BuiltInPlaylist.Device
                            5 -> BuiltInPlaylist.ThisWeek
                            6 -> BuiltInPlaylist.ShortFavorites
                            else -> BuiltInPlaylist.Favorites
                        }
                    )
                }
            }
        }
    }
}
