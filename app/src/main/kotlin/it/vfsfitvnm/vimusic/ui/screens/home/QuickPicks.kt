package it.vfsfitvnm.vimusic.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.compose.persist.persistList
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.NavigationEndpoint
import it.vfsfitvnm.innertube.requests.DefaultLandingVideoId
import it.vfsfitvnm.innertube.requests.landingPage
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.ShimmerHost
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.components.themed.TextPlaceholder
import it.vfsfitvnm.vimusic.ui.items.AlbumItem
import it.vfsfitvnm.vimusic.ui.items.AlbumItemPlaceholder
import it.vfsfitvnm.vimusic.ui.items.ArtistItem
import it.vfsfitvnm.vimusic.ui.items.ArtistItemPlaceholder
import it.vfsfitvnm.vimusic.ui.items.PlaylistItem
import it.vfsfitvnm.vimusic.ui.items.PlaylistItemPlaceholder
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.items.SongItemPlaceholder
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.enums.AppLanguage
import it.vfsfitvnm.vimusic.utils.LocalAppLanguage
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.SnapLayoutInfoProvider
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.color
import it.vfsfitvnm.vimusic.utils.forcePlay
import it.vfsfitvnm.vimusic.utils.isLandscape
import it.vfsfitvnm.vimusic.utils.khatmaMediaIdKey
import it.vfsfitvnm.vimusic.utils.khatmaPositionKey
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeoutException

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun QuickPicks(
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMoodClick: (String) -> Unit = {},
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val strings = LocalStrings.current
    val appLanguage = LocalAppLanguage.current
    val context = LocalContext.current

    var trending by persist<Song?>("home/trending")
    var recentlyPlayed by persistList<Song>("home/recentlyPlayed")
    var khatmaSong by remember { mutableStateOf<Song?>(null) }
    val khatmaId = remember { context.preferences.getString(khatmaMediaIdKey, null) }
    val khatmaPosition = remember { context.preferences.getLong(khatmaPositionKey, 0L) }

    var relatedPageResult by persist<Result<Innertube.RelatedPage>>(tag = "home/relatedPageResult")
    var reloadToken by remember { mutableStateOf(0) }

    suspend fun loadLanding(videoId: String): Result<Innertube.RelatedPage> {
        return withTimeoutOrNull(25_000) {
            Innertube.landingPage(videoId) ?: error("cancelled")
        } ?: Result.failure(TimeoutException("home"))
    }

    LaunchedEffect(reloadToken, appLanguage) {
        if (relatedPageResult == null || reloadToken > 0) {
            val seed = if (appLanguage == AppLanguage.Arabic) {
                DefaultLandingVideoId
            } else {
                trending?.id ?: DefaultLandingVideoId
            }
            relatedPageResult = loadLanding(seed)
        }
    }

    LaunchedEffect(khatmaId) {
        if (!khatmaId.isNullOrEmpty()) {
            Database.song(khatmaId).collect { khatmaSong = it }
        }
    }

    LaunchedEffect(Unit) {
        Database.recentlyPlayed().distinctUntilChanged().collect { songs ->
            recentlyPlayed = songs
        }
    }

    LaunchedEffect(Unit) {
        Database.trending().distinctUntilChanged().collect { song ->
            val changed = trending?.id != song?.id
            trending = song
            if (
                song != null &&
                changed &&
                relatedPageResult != null &&
                appLanguage != AppLanguage.Arabic
            ) {
                relatedPageResult = loadLanding(song.id)
            }
        }
    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = 108.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 108.dp
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val scrollState = rememberScrollState()
    val quickPicksLazyGridState = rememberLazyGridState()

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    BoxWithConstraints {
        val quickPicksLazyGridItemWidthFactor = if (isLandscape && maxWidth * 0.475f >= 320.dp) {
            0.475f
        } else {
            0.9f
        }

        val snapLayoutInfoProvider = remember(quickPicksLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * quickPicksLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }

        val itemInHorizontalGridWidth = maxWidth * quickPicksLazyGridItemWidthFactor

        Column(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Vertical)
                        .asPaddingValues()
                )
        ) {
            Header(
                title = strings.quickPicks,
                modifier = Modifier
                    .padding(endPaddingValues)
            ) {
                SecondaryTextButton(
                    text = strings.refresh,
                    onClick = {
                        relatedPageResult = null
                        reloadToken += 1
                    }
                )
            }

            khatmaSong?.let { song ->
                BasicText(
                    text = strings.continueRecitation,
                    style = typography.m.semiBold,
                    modifier = sectionTextModifier
                )
                SongItem(
                    song = song,
                    thumbnailSizePx = songThumbnailSizePx,
                    thumbnailSizeDp = songThumbnailSizeDp,
                    modifier = Modifier
                        .padding(endPaddingValues)
                        .clickable {
                            binder?.stopRadio()
                            binder?.player?.forcePlay(song.asMediaItem)
                            binder?.player?.seekTo(khatmaPosition)
                        }
                )
            }

            trending?.let { song ->
                BasicText(
                    text = strings.songOfTheDay,
                    style = typography.m.semiBold,
                    modifier = sectionTextModifier
                )
                SongItem(
                    song = song,
                    thumbnailSizePx = songThumbnailSizePx,
                    thumbnailSizeDp = songThumbnailSizeDp,
                    modifier = Modifier
                        .padding(endPaddingValues)
                        .combinedClickable(
                            onLongClick = {
                                menuState.display {
                                    NonQueuedMediaItemMenu(
                                        onDismiss = menuState::hide,
                                        mediaItem = song.asMediaItem
                                    )
                                }
                            },
                            onClick = {
                                binder?.stopRadio()
                                binder?.player?.forcePlay(song.asMediaItem)
                                binder?.setupRadio(
                                    NavigationEndpoint.Endpoint.Watch(videoId = song.id)
                                )
                            }
                        )
                )
            }

            val moods = listOf(
                strings.moodCalm to "موسيقى هادئة",
                strings.moodEnergetic to "أغاني حماسية",
                strings.moodTarab to "طرب عربي",
                strings.moodShaabi to "شعبي مصري",
                strings.moodQuran to "تلاوة قرآن",
                strings.moodFocus to "موسيقى للعمل"
            )

            BasicText(
                text = strings.moods,
                style = typography.m.semiBold,
                modifier = sectionTextModifier
            )

            LazyRow(contentPadding = endPaddingValues) {
                items(moods, key = { it.first }) { (label, query) ->
                    BasicText(
                        text = label,
                        style = typography.xs.semiBold.color(colorPalette.text),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onMoodClick(query) }
                            .background(colorPalette.background2, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            recentlyPlayed.takeIf { it.isNotEmpty() }?.let { recent ->
                BasicText(
                    text = strings.recentlyPlayed,
                    style = typography.m.semiBold,
                    modifier = sectionTextModifier
                )

                LazyRow(contentPadding = endPaddingValues) {
                    items(recent, key = Song::id) { song ->
                        SongItem(
                            song = song,
                            thumbnailSizePx = songThumbnailSizePx,
                            thumbnailSizeDp = songThumbnailSizeDp,
                            modifier = Modifier
                                .width(itemInHorizontalGridWidth)
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = song.asMediaItem
                                            )
                                        }
                                    },
                                    onClick = {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(song.asMediaItem)
                                    }
                                )
                        )
                    }
                }
            }

            relatedPageResult?.getOrNull()?.takeUnless { it.isEmpty }?.let { related ->
                LazyHorizontalGrid(
                    state = quickPicksLazyGridState,
                    rows = GridCells.Fixed(4),
                    flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                    contentPadding = endPaddingValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((songThumbnailSizeDp + Dimensions.itemsVerticalPadding * 2) * 4)
                ) {
                    trending?.let { song ->
                        item {
                            SongItem(
                                song = song,
                                thumbnailSizePx = songThumbnailSizePx,
                                thumbnailSizeDp = songThumbnailSizeDp,
                                trailingContent = {
                                    Image(
                                        painter = painterResource(R.drawable.star),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.accent),
                                        modifier = Modifier
                                            .size(16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    onDismiss = menuState::hide,
                                                    mediaItem = song.asMediaItem,
                                                    onRemoveFromQuickPicks = {
                                                        query {
                                                            Database.clearEventsFor(song.id)
                                                        }
                                                    }
                                                )
                                            }
                                        },
                                        onClick = {
                                            val mediaItem = song.asMediaItem
                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(mediaItem)
                                            binder?.setupRadio(
                                                NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                            )
                                        }
                                    )
                                    .animateItemPlacement()
                                    .width(itemInHorizontalGridWidth)
                            )
                        }
                    }

                    items(
                        items = related.songs?.dropLast(if (trending == null) 0 else 1)
                            ?: emptyList(),
                        key = Innertube.SongItem::key
                    ) { song ->
                        SongItem(
                            song = song,
                            thumbnailSizePx = songThumbnailSizePx,
                            thumbnailSizeDp = songThumbnailSizeDp,
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = song.asMediaItem
                                            )
                                        }
                                    },
                                    onClick = {
                                        val mediaItem = song.asMediaItem
                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(mediaItem)
                                        binder?.setupRadio(
                                            NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                        )
                                    }
                                )
                                .animateItemPlacement()
                                .width(itemInHorizontalGridWidth)
                        )
                    }
                }

                related.albums?.let { albums ->
                    BasicText(
                        text = strings.relatedAlbums,
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = albums,
                            key = Innertube.AlbumItem::key
                        ) { album ->
                            AlbumItem(
                                album = album,
                                thumbnailSizePx = albumThumbnailSizePx,
                                thumbnailSizeDp = albumThumbnailSizeDp,
                                alternative = true,
                                modifier = Modifier
                                    .clickable(onClick = { onAlbumClick(album.key) })
                            )
                        }
                    }
                }

                related.artists?.let { artists ->
                    BasicText(
                        text = strings.similarArtists,
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = artists,
                            key = Innertube.ArtistItem::key,
                        ) { artist ->
                            ArtistItem(
                                artist = artist,
                                thumbnailSizePx = artistThumbnailSizePx,
                                thumbnailSizeDp = artistThumbnailSizeDp,
                                alternative = true,
                                modifier = Modifier
                                    .clickable(onClick = { onArtistClick(artist.key) })
                            )
                        }
                    }
                }

                related.playlists?.let { playlists ->
                    BasicText(
                        text = strings.playlistsYouMightLike,
                        style = typography.m.semiBold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp, bottom = 8.dp)
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = playlists,
                            key = Innertube.PlaylistItem::key,
                        ) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                thumbnailSizePx = playlistThumbnailSizePx,
                                thumbnailSizeDp = playlistThumbnailSizeDp,
                                alternative = true,
                                modifier = Modifier
                                    .clickable(onClick = { onPlaylistClick(playlist.key) })
                            )
                        }
                    }
                }

                Unit
            } ?: relatedPageResult?.let {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                ) {
                    BasicText(
                        text = strings.couldNotLoadHome,
                        style = typography.s.secondary.center
                    )

                    SecondaryTextButton(
                        text = strings.retry,
                        onClick = {
                            relatedPageResult = null
                            reloadToken += 1
                        },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } ?: ShimmerHost {
                repeat(4) {
                    SongItemPlaceholder(
                        thumbnailSizeDp = songThumbnailSizeDp,
                    )
                }

                TextPlaceholder(modifier = sectionTextModifier)

                Row {
                    repeat(2) {
                        AlbumItemPlaceholder(
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true
                        )
                    }
                }

                TextPlaceholder(modifier = sectionTextModifier)

                Row {
                    repeat(2) {
                        ArtistItemPlaceholder(
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true
                        )
                    }
                }

                TextPlaceholder(modifier = sectionTextModifier)

                Row {
                    repeat(2) {
                        PlaylistItemPlaceholder(
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true
                        )
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            scrollState = scrollState,
            iconId = R.drawable.search,
            onClick = onSearchClick
        )
    }
}
