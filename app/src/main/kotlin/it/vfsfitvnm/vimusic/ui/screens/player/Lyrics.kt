package it.vfsfitvnm.vimusic.ui.screens.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import com.valentinilk.shimmer.shimmer
import it.vfsfitvnm.kugou.KuGou
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Lyrics
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.themed.Menu
import it.vfsfitvnm.vimusic.ui.components.themed.MenuEntry
import it.vfsfitvnm.vimusic.ui.components.themed.TextFieldDialog
import it.vfsfitvnm.vimusic.ui.components.themed.TextPlaceholder
import it.vfsfitvnm.vimusic.ui.styling.DefaultDarkColorPalette
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.PureBlackColorPalette
import it.vfsfitvnm.vimusic.ui.styling.onOverlayShimmer
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.SynchronizedLyrics
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.color
import it.vfsfitvnm.vimusic.utils.isBlank
import it.vfsfitvnm.vimusic.utils.isShowingSynchronizedLyricsKey
import it.vfsfitvnm.vimusic.utils.lyricsScaleKey
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.resolveLyrics
import it.vfsfitvnm.vimusic.utils.toast
import it.vfsfitvnm.vimusic.utils.verticalFadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    size: Dp,
    mediaMetadataProvider: () -> MediaMetadata,
    durationProvider: () -> Long,
    ensureSongInserted: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val (colorPalette, typography) = LocalAppearance.current
        val context = LocalContext.current
        val menuState = LocalMenuState.current
        val currentView = LocalView.current
        val strings = LocalStrings.current
        val lyricsScale by rememberPreference(lyricsScaleKey, 1)
        val currentLineStyle = when (lyricsScale) {
            0 -> typography.xs
            2 -> typography.l
            else -> typography.s
        }
        val otherLineStyle = when (lyricsScale) {
            0 -> typography.xxs
            2 -> typography.s
            else -> typography.xs
        }
        val currentWordSp = when (lyricsScale) {
            0 -> 14.sp
            2 -> 20.sp
            else -> 16.sp
        }
        val otherWordSp = when (lyricsScale) {
            0 -> 12.sp
            2 -> 16.sp
            else -> 14.sp
        }

        var preferSynchronizedLyrics by rememberPreference(isShowingSynchronizedLyricsKey, true)

        var isEditing by remember(mediaId, preferSynchronizedLyrics) {
            mutableStateOf(false)
        }

        var lyrics by remember {
            mutableStateOf<Lyrics?>(null)
        }

        val syncedText = lyrics?.synced?.takeIf { it.isNotBlank() }
        val fixedText = lyrics?.fixed?.takeIf { it.isNotBlank() }
        val isShowingSynchronizedLyrics = preferSynchronizedLyrics && syncedText != null ||
            (fixedText == null && syncedText != null)
        val text = if (isShowingSynchronizedLyrics) syncedText else fixedText

        var isError by remember(mediaId) {
            mutableStateOf(false)
        }

        var unavailable by remember(mediaId) {
            mutableStateOf(false)
        }

        LaunchedEffect(mediaId) {
            withContext(Dispatchers.IO) {
                Database.lyrics(mediaId).collect { stored ->
                    if (stored.isBlank()) {
                        val mediaMetadata = mediaMetadataProvider()
                        var duration = withContext(Dispatchers.Main) {
                            durationProvider()
                        }

                        while (duration == C.TIME_UNSET) {
                            delay(100)
                            duration = withContext(Dispatchers.Main) {
                                durationProvider()
                            }
                        }

                        val resolved = runCatching {
                            resolveLyrics(
                                mediaId = mediaId,
                                title = mediaMetadata.title?.toString(),
                                artist = mediaMetadata.artist?.toString(),
                                durationMs = duration
                            )
                        }.getOrElse {
                            isError = true
                            return@collect
                        }

                        if (resolved.hasText) {
                            ensureSongInserted()
                            Database.upsert(
                                Lyrics(
                                    songId = mediaId,
                                    fixed = resolved.fixed,
                                    synced = resolved.synced
                                )
                            )
                        } else {
                            isError = false
                            unavailable = true
                            lyrics = Lyrics(songId = mediaId, fixed = "", synced = "")
                        }
                    } else {
                        isError = false
                        unavailable = false
                        lyrics = stored
                    }
                }
            }
        }

        if (isEditing) {
            TextFieldDialog(
                hintText = strings.enterLyrics,
                initialTextInput = text ?: "",
                singleLine = false,
                maxLines = 10,
                isTextInputValid = { true },
                onDismiss = { isEditing = false },
                onDone = {
                    query {
                        ensureSongInserted()
                        Database.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else it,
                                synced = if (isShowingSynchronizedLyrics) it else lyrics?.synced,
                            )
                        )
                    }
                }
            )
        }

        if (isShowingSynchronizedLyrics) {
            DisposableEffect(Unit) {
                currentView.keepScreenOn = true
                onDispose {
                    currentView.keepScreenOn = false
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() }
                    )
                }
                .fillMaxSize()
                .background(Color.Black.copy(0.8f))
        ) {
            AnimatedVisibility(
                visible = isError && text == null,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                BasicText(
                    text = if (preferSynchronizedLyrics) {
                        strings.synchronizedLyricsFetchError
                    } else {
                        strings.lyricsFetchError
                    },
                    style = typography.xs.center.medium.color(PureBlackColorPalette.text),
                    modifier = Modifier
                        .background(Color.Black.copy(0.4f))
                        .padding(all = 8.dp)
                        .fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = unavailable || text?.isEmpty() == true,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                BasicText(
                    text = if (preferSynchronizedLyrics && syncedText == null && fixedText == null) {
                        strings.lyricsUnavailable
                    } else if (preferSynchronizedLyrics) {
                        strings.synchronizedLyricsUnavailable
                    } else {
                        strings.lyricsUnavailable
                    },
                    style = typography.xs.center.medium.color(PureBlackColorPalette.text),
                    modifier = Modifier
                        .background(Color.Black.copy(0.4f))
                        .padding(all = 8.dp)
                        .fillMaxWidth()
                )
            }

            if (text?.isNotEmpty() == true) {
                if (isShowingSynchronizedLyrics) {
                    val density = LocalDensity.current
                    val player = LocalPlayerServiceBinder.current?.player
                        ?: return@AnimatedVisibility

                    val synchronizedLyrics = remember(text) {
                        SynchronizedLyrics(KuGou.Lyrics(text).lines) {
                            player.currentPosition + 50
                        }
                    }

                    val lazyListState = rememberLazyListState(
                        synchronizedLyrics.index,
                        with(density) { size.roundToPx() } / 6
                    )

                    LaunchedEffect(synchronizedLyrics) {
                        val center = with(density) { size.roundToPx() } / 6

                        while (isActive) {
                            delay(50)
                            if (synchronizedLyrics.update()) {
                                lazyListState.animateScrollToItem(
                                    synchronizedLyrics.index,
                                    center
                                )
                            }
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        userScrollEnabled = false,
                        contentPadding = PaddingValues(vertical = size / 2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .verticalFadingEdge()
                    ) {
                        itemsIndexed(items = synchronizedLyrics.lines) { index, line ->
                            val isCurrent = index == synchronizedLyrics.index
                            val alreadySung = index < synchronizedLyrics.index
                            val words = line.words
                            val style = if (isCurrent) {
                                currentLineStyle.center.medium.color(PureBlackColorPalette.text)
                            } else {
                                otherLineStyle.center.medium.color(
                                    if (alreadySung) {
                                        PureBlackColorPalette.text.copy(alpha = 0.38f)
                                    } else {
                                        PureBlackColorPalette.textDisabled
                                    }
                                )
                            }

                            val displayText = if (isCurrent && words.isNotEmpty()) {
                                buildAnnotatedString {
                                    words.forEachIndexed { wordIndex, word ->
                                        val sung = wordIndex <= synchronizedLyrics.wordIndex
                                        withStyle(
                                            SpanStyle(
                                                color = if (sung) {
                                                    PureBlackColorPalette.text
                                                } else {
                                                    PureBlackColorPalette.textDisabled
                                                },
                                                fontSize = if (wordIndex == synchronizedLyrics.wordIndex) {
                                                    currentWordSp
                                                } else {
                                                    otherWordSp
                                                }
                                            )
                                        ) {
                                            append(word.text)
                                        }
                                        if (wordIndex != words.lastIndex) append(" ")
                                    }
                                }
                            } else {
                                buildAnnotatedString { append(line.text) }
                            }

                            BasicText(
                                text = displayText,
                                style = style,
                                modifier = Modifier
                                    .padding(vertical = 6.dp, horizontal = 32.dp)
                                    .alpha(if (alreadySung) 0.7f else 1f)
                            )
                        }
                    }
                } else {
                    BasicText(
                        text = text,
                        style = otherLineStyle.center.medium.color(PureBlackColorPalette.text),
                        modifier = Modifier
                            .verticalFadingEdge()
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .padding(vertical = size / 4, horizontal = 32.dp)
                    )
                }
            }

            if (text == null && !isError && !unavailable) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .shimmer()
                ) {
                    repeat(4) {
                        TextPlaceholder(
                            color = colorPalette.onOverlayShimmer,
                            modifier = Modifier
                                .alpha(1f - it * 0.2f)
                        )
                    }
                }
            }

            Image(
                painter = painterResource(R.drawable.ellipsis_horizontal),
                contentDescription = null,
                colorFilter = ColorFilter.tint(DefaultDarkColorPalette.text),
                modifier = Modifier
                    .padding(all = 4.dp)
                    .clickable(
                        indication = rememberRipple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            menuState.display {
                                Menu {
                                    MenuEntry(
                                        icon = R.drawable.time,
                                        text = if (preferSynchronizedLyrics) {
                                            strings.showUnsynchronizedLyrics
                                        } else {
                                            strings.showSynchronizedLyrics
                                        },
                                        secondaryText = if (preferSynchronizedLyrics) null else strings.providedByKugou,
                                        onClick = {
                                            menuState.hide()
                                            preferSynchronizedLyrics = !preferSynchronizedLyrics
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.pencil,
                                        text = strings.editLyrics,
                                        onClick = {
                                            menuState.hide()
                                            isEditing = true
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.search,
                                        text = strings.searchLyricsOnline,
                                        onClick = {
                                            menuState.hide()
                                            val mediaMetadata = mediaMetadataProvider()

                                            try {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                        putExtra(
                                                            SearchManager.QUERY,
                                                            "${mediaMetadata.title} ${mediaMetadata.artist} lyrics"
                                                        )
                                                    }
                                                )
                                            } catch (e: ActivityNotFoundException) {
                                                context.toast(strings.browseInternetMissing)
                                            }
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.download,
                                        text = strings.fetchLyricsAgain,
                                        enabled = lyrics != null,
                                        onClick = {
                                            menuState.hide()
                                            lyrics = null
                                            isError = false
                                            unavailable = false
                                            query {
                                                Database.upsert(
                                                    Lyrics(
                                                        songId = mediaId,
                                                        fixed = null,
                                                        synced = null,
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    )
                    .padding(all = 8.dp)
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}
