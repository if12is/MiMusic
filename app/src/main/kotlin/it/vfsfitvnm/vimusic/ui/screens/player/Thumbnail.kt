package it.vfsfitvnm.vimusic.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import coil.compose.AsyncImage
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.service.LoginRequiredException
import it.vfsfitvnm.vimusic.service.PlayableFormatNotFoundException
import it.vfsfitvnm.vimusic.service.UnplayableException
import it.vfsfitvnm.vimusic.service.VideoIdMismatchException
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.PlaybackLogStore
import it.vfsfitvnm.vimusic.utils.currentWindow
import it.vfsfitvnm.vimusic.utils.DisposableListener
import it.vfsfitvnm.vimusic.utils.forceSeekToNext
import it.vfsfitvnm.vimusic.utils.forceSeekToPrevious
import it.vfsfitvnm.vimusic.utils.thumbnail
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.visualizerEnabledKey
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

@ExperimentalAnimationApi
@Composable
fun Thumbnail(
    isShowingLyrics: Boolean,
    onShowLyrics: (Boolean) -> Unit,
    isShowingStatsForNerds: Boolean,
    onShowStatsForNerds: (Boolean) -> Unit,
    onSwipeCollapse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val strings = LocalStrings.current

    val (thumbnailSizeDp, thumbnailSizePx) = Dimensions.thumbnails.player.song.let {
        it to (it - 64.dp).px
    }

    var nullableWindow by remember {
        mutableStateOf(player.currentWindow)
    }

    var error by remember {
        mutableStateOf<PlaybackException?>(player.playerError)
    }

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableWindow = player.currentWindow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                error = player.playerError
            }

            override fun onPlayerError(playbackException: PlaybackException) {
                error = playbackException
            }
        }
    }

    val window = nullableWindow ?: return

    AnimatedContent(
        targetState = window,
        transitionSpec = {
            val duration = 500
            val slideDirection =
                if (targetState.firstPeriodIndex > initialState.firstPeriodIndex) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right

            ContentTransform(
                targetContentEnter = slideIntoContainer(
                    towards = slideDirection,
                    animationSpec = tween(duration)
                ) + fadeIn(
                    animationSpec = tween(duration)
                ) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(duration)
                ),
                initialContentExit = slideOutOfContainer(
                    towards = slideDirection,
                    animationSpec = tween(duration)
                ) + fadeOut(
                    animationSpec = tween(duration)
                ) + scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(duration)
                ),
                sizeTransform = SizeTransform(clip = false)
            )
        },
        contentAlignment = Alignment.Center
    ) {currentWindow ->
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(LocalAppearance.current.thumbnailShape)
                .size(thumbnailSizeDp)
        ) {
            AsyncImage(
                model = currentWindow.mediaItem.mediaMetadata.artworkUri.thumbnail(
                    thumbnailSizePx,
                    currentWindow.mediaItem.mediaId
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onShowLyrics(true) },
                            onLongPress = { onShowStatsForNerds(true) }
                        )
                    }
                    .pointerInput(player) {
                        var acc = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    acc > 80f -> player.forceSeekToPrevious()
                                    acc < -80f -> player.forceSeekToNext()
                                }
                                acc = 0f
                            },
                            onHorizontalDrag = { _, delta -> acc += delta }
                        )
                    }
                    .pointerInput(Unit) {
                        var acc = 0f
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (acc > 80f) onSwipeCollapse()
                                acc = 0f
                            },
                            onVerticalDrag = { _, delta -> acc += delta }
                        )
                    }
                    .fillMaxSize()
            )

            val visualizerEnabled by rememberPreference(visualizerEnabledKey, false)
            val lowPowerMode by rememberPreference(it.vfsfitvnm.vimusic.utils.lowPowerModeKey, false)
            if (visualizerEnabled && !lowPowerMode && !isShowingLyrics) {
                LightVisualizer(
                    color = LocalAppearance.current.colorPalette.accent,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxSize()
                )
            }

            Lyrics(
                mediaId = currentWindow.mediaItem.mediaId,
                isDisplayed = isShowingLyrics && error == null,
                onDismiss = { onShowLyrics(false) },
                ensureSongInserted = { Database.insert(currentWindow.mediaItem) },
                size = thumbnailSizeDp,
                mediaMetadataProvider = currentWindow.mediaItem::mediaMetadata,
                durationProvider = player::getDuration,
            )

            StatsForNerds(
                mediaId = currentWindow.mediaItem.mediaId,
                isDisplayed = isShowingStatsForNerds && error == null,
                onDismiss = { onShowStatsForNerds(false) }
            )

            PlaybackError(
                isDisplayed = error != null,
                messageProvider = {
                    val causes = generateSequence(error as Throwable?) { it.cause }.toList()
                    when {
                        causes.any { it is UnresolvedAddressException || it is UnknownHostException } ->
                            strings.networkError
                        causes.any { it is PlayableFormatNotFoundException } ->
                            strings.playableFormatNotFound
                        causes.any { it is UnplayableException } ->
                            strings.unplayable
                        causes.any { it is LoginRequiredException } ->
                            strings.loginRequired
                        causes.any { it is VideoIdMismatchException } ->
                            strings.videoIdMismatch
                        else -> {
                            val summary = PlaybackLogStore.lastSummary()
                            if (summary.isBlank()) {
                                strings.unknownPlaybackError
                            } else {
                                "${strings.unknownPlaybackError}\n$summary"
                            }
                        }
                    }
                },
                onDismiss = player::prepare
            )
        }
    }
}

@Composable
private fun LightVisualizer(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "visualizer")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "visualizerT"
    )

    Canvas(modifier = modifier) {
        val bars = 14
        val gap = size.width / (bars * 2f)
        repeat(bars) { index ->
            val height = size.height * (
                0.18f + 0.62f * kotlin.math.abs(kotlin.math.sin((t * 7f + index) * 0.65f))
            )
            drawRect(
                color = color.copy(alpha = 0.42f),
                topLeft = Offset(x = index * gap * 2f, y = size.height - height),
                size = Size(gap, height)
            )
        }
    }
}
