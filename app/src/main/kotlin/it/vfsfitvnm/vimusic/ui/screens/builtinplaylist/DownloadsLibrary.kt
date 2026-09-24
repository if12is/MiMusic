package it.vfsfitvnm.vimusic.ui.screens.builtinplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.SongWithContentLength
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.DownloadSort
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.forcePlayAtIndex
import it.vfsfitvnm.vimusic.utils.forcePlayFromBeginning
import it.vfsfitvnm.vimusic.utils.formatByteSize
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.smartShuffleKey
import it.vfsfitvnm.vimusic.utils.smartShuffled
import it.vfsfitvnm.vimusic.utils.sortedDownloads
import it.vfsfitvnm.vimusic.utils.totalDownloadBytes
import it.vfsfitvnm.vimusic.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun DownloadsLibrary() {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val strings = LocalStrings.current
    val smartShuffle by rememberPreference(smartShuffleKey, true)
    var stored by persistList<SongWithContentLength>("downloads/library")
    var sortName by rememberSaveable { mutableStateOf(DownloadSort.Recent.name) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptyList<String>()) }
    val sort = runCatching { DownloadSort.valueOf(sortName) }.getOrDefault(DownloadSort.Recent)

    LaunchedEffect(Unit) {
        Database.downloadedSongs()
            .flowOn(Dispatchers.IO)
            .map { songs ->
                songs.filter { song ->
                    song.contentLength?.let { length ->
                        binder?.isAvailableOffline(song.song.id, length) == true
                    } ?: (binder?.isDownloaded(song.song.id) == true)
                }
            }
            .collect { stored = it }
    }

    val songs = stored.sortedDownloads(sort)
    val mediaItems = songs.map { it.song.asMediaItem }
    val thumbnailSizeDp = Dimensions.thumbnails.song
    val lazyListState = rememberLazyListState()

    Box {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues(),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            item(key = "header", contentType = 0) {
                Header(title = strings.offline) {
                    SecondaryTextButton(
                        text = strings.playAll,
                        enabled = songs.isNotEmpty() && !selecting,
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(mediaItems)
                        }
                    )
                    SecondaryTextButton(
                        text = if (selecting) strings.cancel else strings.selectItems,
                        enabled = songs.isNotEmpty() || selecting,
                        onClick = {
                            selecting = !selecting
                            selected = emptyList()
                        }
                    )
                    if (selecting) {
                        SecondaryTextButton(
                            text = strings.delete,
                            enabled = selected.isNotEmpty(),
                            onClick = {
                                val ids = selected.toSet()
                                ids.forEach { id -> binder?.removeDownload(id) }
                                stored = stored.filter { it.song.id !in ids }
                                selected = emptyList()
                                selecting = false
                            }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            item(key = "summary") {
                BasicText(
                    text = strings.downloadedSize(formatByteSize(totalDownloadBytes(songs))),
                    style = typography.xxs.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                    SortChip(strings.sortRecent, sort == DownloadSort.Recent) {
                        sortName = DownloadSort.Recent.name
                    }
                    SortChip(strings.sortTitle, sort == DownloadSort.Title) {
                        sortName = DownloadSort.Title.name
                    }
                    SortChip(strings.sortSize, sort == DownloadSort.Size) {
                        sortName = DownloadSort.Size.name
                    }
                }
            }

            itemsIndexed(songs, key = { _, song -> song.song.id }) { index, song ->
                val picked = song.song.id in selected
                SongItem(
                    thumbnailUrl = song.song.thumbnailUrl,
                    title = song.song.title,
                    authors = listOfNotNull(
                        song.song.artistsText,
                        song.contentLength?.let(::formatByteSize)
                    ).joinToString(" · "),
                    duration = song.song.durationText,
                    thumbnailSizeDp = thumbnailSizeDp,
                    modifier = Modifier.combinedClickable(
                        onLongClick = {
                            selecting = true
                            selected = (selected + song.song.id).distinct()
                        },
                        onClick = {
                            if (selecting) {
                                selected = if (picked) selected - song.song.id else selected + song.song.id
                            } else {
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(mediaItems, index)
                            }
                        }
                    ),
                    trailingContent = if (!selecting) {
                        null
                    } else {
                        {
                            BasicText(
                                text = if (picked) "✓" else "·",
                                style = typography.s.copy(color = colorPalette.accent),
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = R.drawable.shuffle,
            onClick = {
                if (mediaItems.isNotEmpty()) {
                    binder?.stopRadio()
                    binder?.player?.forcePlayFromBeginning(mediaItems.smartShuffled(smartShuffle))
                }
            }
        )
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val (colorPalette, typography) = LocalAppearance.current
    BasicText(
        text = label,
        style = typography.xxs.semiBold.copy(
            color = if (selected) colorPalette.accent else colorPalette.textSecondary
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    )
}
