package it.vfsfitvnm.vimusic.ui.screens.builtinplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.BuiltInPlaylist
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.models.SongWithContentLength
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.InHistoryMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.px
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.asLocalMediaItem
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.enqueue
import it.vfsfitvnm.vimusic.utils.forcePlayAtIndex
import it.vfsfitvnm.vimusic.utils.forcePlayFromBeginning
import it.vfsfitvnm.vimusic.utils.queryDeviceSongs
import it.vfsfitvnm.vimusic.utils.smartShuffled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun BuiltInPlaylistSongs(builtInPlaylist: BuiltInPlaylist) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val strings = LocalStrings.current
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(builtInPlaylist) {
        if (builtInPlaylist == BuiltInPlaylist.Device &&
            ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(permission)
        }
    }

    var songs by persistList<Song>("${builtInPlaylist.name}/songs")

    LaunchedEffect(builtInPlaylist) {
        when (builtInPlaylist) {
            BuiltInPlaylist.Favorites -> Database.favorites()
            BuiltInPlaylist.History -> Database.playbackHistory()
            BuiltInPlaylist.Top -> Database.mostPlayed()
            BuiltInPlaylist.Device -> flow { emit(context.queryDeviceSongs()) }
            BuiltInPlaylist.Offline -> Database
                .downloadedSongs()
                .flowOn(Dispatchers.IO)
                .map { songs ->
                    songs.filter { song ->
                        song.contentLength?.let { length ->
                            binder?.isAvailableOffline(song.song.id, length) == true
                        } ?: (binder?.isDownloaded(song.song.id) == true)
                    }.map(SongWithContentLength::song)
                }
        }.collect { songs = it }
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSize = thumbnailSizeDp.px

    val lazyListState = rememberLazyListState()

    Box {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = 0
            ) {
                Header(
                    title = when (builtInPlaylist) {
                        BuiltInPlaylist.Favorites -> strings.favorites
                        BuiltInPlaylist.Offline -> strings.offline
                        BuiltInPlaylist.History -> strings.playbackHistory
                        BuiltInPlaylist.Top -> strings.mostPlayed
                        BuiltInPlaylist.Device -> strings.onDevice
                    },
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                ) {
                    SecondaryTextButton(
                        text = strings.enqueue,
                        enabled = songs.isNotEmpty(),
                            onClick = {
                            val mediaItems = songs.map { song ->
                                if (song.id.startsWith("local:")) song.asLocalMediaItem() else song.asMediaItem
                            }
                            binder?.player?.enqueue(mediaItems)
                        }
                    )

                    if (builtInPlaylist == BuiltInPlaylist.Favorites) {
                        SecondaryTextButton(
                            text = strings.downloadFavorites,
                            enabled = songs.isNotEmpty(),
                            onClick = {
                                binder?.downloadAll(songs.map(Song::asMediaItem))
                            }
                        )
                    }

                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                    )
                }
            }

            itemsIndexed(
                items = songs,
                key = { _, song -> song.id },
                contentType = { _, song -> song },
            ) { index, song ->
                SongItem(
                    song = song,
                    thumbnailSizeDp = thumbnailSizeDp,
                    thumbnailSizePx = thumbnailSize,
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                menuState.display {
                                    when (builtInPlaylist) {
                                        BuiltInPlaylist.Favorites -> NonQueuedMediaItemMenu(
                                            mediaItem = song.asMediaItem,
                                            onDismiss = menuState::hide
                                        )

                                        BuiltInPlaylist.Offline, BuiltInPlaylist.History, BuiltInPlaylist.Top, BuiltInPlaylist.Device -> InHistoryMediaItemMenu(
                                            song = song,
                                            onDismiss = menuState::hide
                                        )
                                    }
                                }
                            },
                            onClick = {
                                val mediaItems = songs.map { song ->
                                    if (song.id.startsWith("local:")) song.asLocalMediaItem() else song.asMediaItem
                                }
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(mediaItems, index)
                            }
                        )
                        .animateItemPlacement()
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = R.drawable.shuffle,
            onClick = {
                if (songs.isNotEmpty()) {
                    binder?.stopRadio()
                    binder?.player?.forcePlayFromBeginning(
                        songs.map { song ->
                            if (song.id.startsWith("local:")) song.asLocalMediaItem() else song.asMediaItem
                        }.smartShuffled()
                    )
                }
            }
        )
    }
}
