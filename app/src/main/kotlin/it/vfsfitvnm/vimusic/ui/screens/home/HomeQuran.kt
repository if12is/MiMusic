package it.vfsfitvnm.vimusic.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.innertube.models.NavigationEndpoint
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.InHistoryMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.color
import it.vfsfitvnm.vimusic.utils.forcePlay
import it.vfsfitvnm.vimusic.utils.forcePlayAtIndex
import it.vfsfitvnm.vimusic.utils.khatmaMediaIdKey
import it.vfsfitvnm.vimusic.utils.khatmaPositionKey
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.smartShuffleKey
import it.vfsfitvnm.vimusic.utils.smartShuffled

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeQuran(
    onSearchClick: (String) -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val strings = LocalStrings.current
    val smartShuffle by rememberPreference(smartShuffleKey, true)
    val context = LocalContext.current

    var songs by persistList<Song>("home/quran/songs")
    var khatmaSong by remember { mutableStateOf<Song?>(null) }
    val khatmaId = remember { context.preferences.getString(khatmaMediaIdKey, null) }
    val khatmaPosition = remember { context.preferences.getLong(khatmaPositionKey, 0L) }

    LaunchedEffect(Unit) {
        Database.quranSongs().collect { songs = it }
    }

    LaunchedEffect(khatmaId) {
        if (!khatmaId.isNullOrEmpty()) {
            Database.song(khatmaId).collect { khatmaSong = it }
        }
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px
    val lazyListState = rememberLazyListState()

    val reciters = listOf(
        strings.moodQuran to "تلاوة قرآن",
        strings.quranMishary to "مشاري العفاسي",
        strings.quranMinshawi to "المنشاوي",
        strings.quranHosary to "الحصري",
        strings.quranSudais to "السديس"
    )

    Box {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            item(key = "header", contentType = 0) {
                Header(title = strings.moodQuran) {
                    SecondaryTextButton(
                        text = strings.search,
                        onClick = { onSearchClick("تلاوة قرآن") }
                    )
                }
            }

            item(key = "reciters") {
                LazyRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    items(reciters, key = { it.first }) { (label, query) ->
                        BasicText(
                            text = label,
                            style = typography.xs.semiBold.color(colorPalette.text),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colorPalette.background2)
                                .clickable { onSearchClick(query) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            khatmaSong?.let { song ->
                item(key = "khatma") {
                    BasicText(
                        text = strings.continueRecitation,
                        style = typography.m.semiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    SongItem(
                        song = song,
                        thumbnailSizePx = thumbnailSizePx,
                        thumbnailSizeDp = thumbnailSizeDp,
                        modifier = Modifier.clickable {
                            binder?.stopRadio()
                            binder?.player?.forcePlay(song.asMediaItem)
                            binder?.player?.seekTo(khatmaPosition)
                        }
                    )
                }
            }

            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                SongItem(
                    song = song,
                    thumbnailSizeDp = thumbnailSizeDp,
                    thumbnailSizePx = thumbnailSizePx,
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                menuState.display {
                                    InHistoryMediaItemMenu(
                                        song = song,
                                        onDismiss = menuState::hide
                                    )
                                }
                            },
                            onClick = {
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(
                                    songs.map(Song::asMediaItem),
                                    index
                                )
                                binder?.setupRadio(
                                    NavigationEndpoint.Endpoint.Watch(videoId = song.id)
                                )
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
                    binder?.player?.forcePlayAtIndex(songs.map(Song::asMediaItem).smartShuffled(smartShuffle), 0)
                } else {
                    onSearchClick("تلاوة قرآن")
                }
            }
        )
    }
}
