package it.vfsfitvnm.vimusic.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.bodies.SearchBody
import it.vfsfitvnm.innertube.requests.searchPage
import it.vfsfitvnm.innertube.utils.ExtraTrack
import it.vfsfitvnm.innertube.utils.from
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.vfsfitvnm.vimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.forcePlay
import it.vfsfitvnm.vimusic.utils.parsePodcastFeedUrls
import it.vfsfitvnm.vimusic.utils.podcastFeedsKey
import it.vfsfitvnm.vimusic.utils.podcastTracks
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.quranSearchQuery
import it.vfsfitvnm.vimusic.utils.searchExtraSources
import it.vfsfitvnm.vimusic.utils.secondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

enum class UnifiedSearchMode {
    All,
    Podcasts,
    Quran
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun UnifiedSearchResults(
    query: String,
    mode: UnifiedSearchMode,
    headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val strings = LocalStrings.current
    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px
    val lazyListState = rememberLazyListState()

    var songs by remember(query, mode) { mutableStateOf<List<Innertube.SongItem>>(emptyList()) }
    var extras by remember(query, mode) { mutableStateOf<List<ExtraTrack>>(emptyList()) }
    var loading by remember(query, mode) { mutableStateOf(true) }

    LaunchedEffect(query, mode) {
        loading = true
        val loaded = withContext(Dispatchers.IO) {
            supervisorScope {
                val youtube = async {
                    val text = if (mode == UnifiedSearchMode.Quran) quranSearchQuery(query) else query
                    if (mode == UnifiedSearchMode.Podcasts) {
                        emptyList()
                    } else {
                        runCatching {
                            Innertube.searchPage(
                                body = SearchBody(query = text, params = Innertube.SearchFilter.Song.value),
                                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                            )?.getOrNull()?.items.orEmpty()
                        }.getOrDefault(emptyList())
                    }
                }
                val extra = async {
                    if (mode == UnifiedSearchMode.Quran) {
                        emptyList()
                    } else {
                        runCatching {
                            val feeds = parsePodcastFeedUrls(
                                context.preferences.getString(podcastFeedsKey, "").orEmpty()
                            )
                            val tracks = searchExtraSources(query, feeds, context.preferences)
                            if (mode == UnifiedSearchMode.Podcasts) podcastTracks(tracks) else tracks
                        }.getOrDefault(emptyList())
                    }
                }
                youtube.await() to extra.await()
            }
        }
        songs = loaded.first
        extras = loaded.second
        loading = false
    }

    Box {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header") { headerContent(null) }

            if (loading) {
                item(key = "loading") {
                    BasicText(
                        text = strings.search,
                        style = typography.s.secondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (songs.isEmpty() && extras.isEmpty()) {
                item(key = "empty") {
                    BasicText(
                        text = strings.noResults,
                        style = typography.s.secondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(songs, key = { "yt-${it.key}" }) { song ->
                SongItem(
                    song = song,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    modifier = Modifier.combinedClickable(
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
                            binder?.setupRadio(song.info?.endpoint)
                        }
                    )
                )
            }

            items(extras, key = { "ex-${it.mediaId}" }) { track ->
                SongItem(
                    thumbnailUrl = track.thumbnailUrl,
                    title = track.title,
                    authors = listOfNotNull(track.artist, track.source).joinToString(" · "),
                    duration = track.durationText,
                    thumbnailSizeDp = thumbnailSizeDp,
                    modifier = Modifier.combinedClickable(
                        onLongClick = {},
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlay(track.asMediaItem())
                        }
                    )
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
    }
}
