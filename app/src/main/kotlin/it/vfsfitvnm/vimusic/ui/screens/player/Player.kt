@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package it.vfsfitvnm.vimusic.ui.screens.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import it.vfsfitvnm.vimusic.ui.components.themed.Artwork
import it.vfsfitvnm.innertube.models.NavigationEndpoint
import it.vfsfitvnm.compose.routing.OnGlobalRoute
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.service.DeviceCast
import it.vfsfitvnm.vimusic.service.PlayerService
import it.vfsfitvnm.vimusic.ui.components.BottomSheet
import it.vfsfitvnm.vimusic.ui.components.BottomSheetState
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.rememberBottomSheetState
import it.vfsfitvnm.vimusic.ui.components.themed.BaseMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.IconButton
import it.vfsfitvnm.vimusic.enums.NavigationStyle
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.collapsedPlayerProgressBar
import it.vfsfitvnm.vimusic.ui.styling.px
import it.vfsfitvnm.vimusic.utils.forceSeekToNext
import it.vfsfitvnm.vimusic.utils.isLandscape
import it.vfsfitvnm.vimusic.utils.navigationStyleKey
import it.vfsfitvnm.vimusic.utils.positionAndDurationState
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.seamlessPlay
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.thumbnail
import it.vfsfitvnm.vimusic.utils.toast
import it.vfsfitvnm.vimusic.utils.LocalStrings
import android.os.Build
import kotlin.math.absoluteValue

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun Player(
    layoutState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val menuState = LocalMenuState.current

    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    binder?.player ?: return

    val session = koinViewModel<PlayerSessionModel>()
    session.attach(binder.player)
    val mediaItem = session.mediaItem ?: return
    val shouldBePlaying = session.shouldBePlaying

    val positionAndDuration by binder.player.positionAndDurationState()

    val windowInsets = WindowInsets.systemBars
    val navigationStyle by rememberPreference(navigationStyleKey, NavigationStyle.Side)
    val isGlassNavigation = navigationStyle == NavigationStyle.GlassBottom

    val horizontalBottomPaddingValues = windowInsets
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()
    val horizontalPaddingValues = windowInsets
        .only(WindowInsetsSides.Horizontal)
        .asPaddingValues()

    OnGlobalRoute {
        layoutState.collapseSoft()
    }

    BottomSheet(
        state = layoutState,
        modifier = modifier,
        onDismiss = {
            binder.stopRadio()
            binder.player.clearMediaItems()
        },
        collapsedContent = {
            val collapsedRowModifier = Modifier
                .background(colorPalette.background1)
                .then(
                    if (isGlassNavigation) {
                        Modifier
                            .fillMaxWidth()
                            .height(Dimensions.collapsedPlayer)
                            .padding(horizontalPaddingValues)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .padding(horizontalBottomPaddingValues)
                    }
                )
                .drawBehind {
                    val duration = positionAndDuration.second.absoluteValue
                    val progress = if (duration == 0L) {
                        0f
                    } else {
                        (positionAndDuration.first.toFloat() / duration).coerceIn(0f, 1f)
                    }

                    drawLine(
                        color = colorPalette.collapsedPlayerProgressBar,
                        start = Offset(x = 0f, y = 1.dp.toPx()),
                        end = Offset(x = size.width * progress, y = 1.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }

            if (isGlassNavigation) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CollapsedMiniPlayer(
                        mediaItem = mediaItem,
                        shouldBePlaying = shouldBePlaying,
                        binder = binder,
                        modifier = collapsedRowModifier
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                CollapsedMiniPlayer(
                    mediaItem = mediaItem,
                    shouldBePlaying = shouldBePlaying,
                    binder = binder,
                    modifier = collapsedRowModifier
                )
            }
        }
    ) {
        var isShowingLyrics by rememberSaveable {
            mutableStateOf(false)
        }

        var isShowingStatsForNerds by rememberSaveable {
            mutableStateOf(false)
        }

        var playerLocked by rememberSaveable {
            mutableStateOf(false)
        }

        val strings = LocalStrings.current
        val context = LocalContext.current

        val playerBottomSheetState = rememberBottomSheetState(
            64.dp + horizontalBottomPaddingValues.calculateBottomPadding(),
            layoutState.expandedBound
        )

        val containerModifier = Modifier
            .background(colorPalette.background1)
            .padding(
                windowInsets
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    .asPaddingValues()
            )
            .padding(bottom = playerBottomSheetState.collapsedBound)

        val thumbnailContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
            Thumbnail(
                isShowingLyrics = isShowingLyrics,
                onShowLyrics = { isShowingLyrics = it },
                isShowingStatsForNerds = isShowingStatsForNerds,
                onShowStatsForNerds = { isShowingStatsForNerds = it },
                onSwipeCollapse = layoutState::collapseSoft,
                modifier = modifier
                    .nestedScroll(layoutState.preUpPostDownNestedScrollConnection)
            )
        }

        val controlsContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
            Controls(
                mediaId = mediaItem.mediaId,
                title = mediaItem.mediaMetadata.title?.toString(),
                artist = mediaItem.mediaMetadata.artist?.toString(),
                shouldBePlaying = shouldBePlaying,
                position = positionAndDuration.first,
                duration = positionAndDuration.second,
                onShowLyrics = { isShowingLyrics = true },
                onShowSleepTimer = {
                    menuState.display {
                        PlayerMenu(
                            onDismiss = menuState::hide,
                            mediaItem = mediaItem,
                            binder = binder,
                            onShowLyrics = { isShowingLyrics = true }
                        )
                    }
                },
                modifier = modifier
            )
        }

        if (isLandscape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = containerModifier
                    .padding(top = 32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(0.66f)
                        .padding(bottom = 16.dp)
                ) {
                    thumbnailContent(
                        Modifier
                            .padding(horizontal = 16.dp)
                    )
                }

                controlsContent(
                    Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxHeight()
                        .weight(1f)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = containerModifier
                    .padding(top = 54.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1.25f)
                ) {
                    thumbnailContent(
                        Modifier
                            .padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                }

                controlsContent(
                    Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }


        Queue(
            layoutState = playerBottomSheetState,
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.playlist),
                            contentDescription = strings.queue,
                            colorFilter = ColorFilter.tint(colorPalette.text),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    StripIcon(
                        icon = R.drawable.ellipsis_horizontal,
                        description = strings.moreOptions,
                        tint = colorPalette.text,
                        onClick = {
                            menuState.display {
                                PlayerMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = mediaItem,
                                    binder = binder,
                                    onShowLyrics = { isShowingLyrics = true }
                                )
                            }
                        }
                    )

                    StripIcon(
                        icon = R.drawable.cast,
                        description = strings.cast,
                        tint = colorPalette.text,
                        onClick = {
                            val started = DeviceCast.request(
                                context = context,
                                localPlayer = binder.player,
                                mediaItem = mediaItem,
                                resolve = binder::castableUri
                            )
                            if (!started) context.toast(strings.castUnavailable)
                        }
                    )

                    StripIcon(
                        icon = R.drawable.film,
                        description = strings.pictureInPicture,
                        tint = colorPalette.text,
                        onClick = {
                            val activity = context as? android.app.Activity
                            if (activity != null && Build.VERSION.SDK_INT >= 26) {
                                activity.enterPictureInPictureMode(
                                    android.app.PictureInPictureParams.Builder().build()
                                )
                            }
                        }
                    )

                    StripIcon(
                        icon = R.drawable.lock,
                        description = strings.playerLock,
                        tint = if (playerLocked) colorPalette.accent else colorPalette.text,
                        onClick = { playerLocked = !playerLocked }
                    )
                }
            },
            backgroundColorProvider = { colorPalette.background2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )

        if (playerLocked) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorPalette.background0.copy(alpha = 0.72f))
                    .clickable { playerLocked = false }
            ) {
                BasicText(
                    text = strings.unlockPlayer,
                    style = typography.s.semiBold
                )
            }
        }
    }
}

@Composable
private fun CollapsedMiniPlayer(
    mediaItem: MediaItem,
    shouldBePlaying: Boolean,
    binder: PlayerService.Binder,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier
    ) {
        Spacer(
            modifier = Modifier
                .width(2.dp)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(Dimensions.collapsedPlayer)
        ) {
            Artwork(
                data = mediaItem.mediaMetadata.artworkUri.thumbnail(Dimensions.thumbnails.song.px),
                sizePx = Dimensions.thumbnails.song.px,
                modifier = Modifier.size(48.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .height(Dimensions.collapsedPlayer)
                .weight(1f)
        ) {
            BasicText(
                text = mediaItem.mediaMetadata.title?.toString() ?: "",
                style = typography.xs.semiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                style = typography.xs.semiBold.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(
            modifier = Modifier
                .width(2.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(Dimensions.collapsedPlayer)
        ) {
            IconButton(
                icon = if (shouldBePlaying) R.drawable.pause else R.drawable.play,
                color = colorPalette.text,
                onClick = {
                    if (shouldBePlaying) {
                        binder.player.pause()
                    } else {
                        if (binder.player.playbackState == Player.STATE_IDLE) {
                            binder.player.prepare()
                        }
                        binder.player.play()
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .size(20.dp)
            )

            IconButton(
                icon = R.drawable.play_skip_forward,
                color = colorPalette.text,
                onClick = binder.player::forceSeekToNext,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .size(20.dp)
            )
        }

        Spacer(
            modifier = Modifier
                .width(2.dp)
        )
    }
}

@Composable
private fun StripIcon(
    icon: Int,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(22.dp)
        )
    }
}

@ExperimentalAnimationApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun PlayerMenu(
    binder: PlayerService.Binder,
    mediaItem: MediaItem,
    onDismiss: () -> Unit,
    onShowLyrics: () -> Unit = {},
) {
    val context = LocalContext.current
    val strings = it.vfsfitvnm.vimusic.utils.LocalStrings.current

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onShowLyrics = onShowLyrics,
        onStartRadio = {
            binder.stopRadio()
            binder.player.seamlessPlay(mediaItem)
            binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
        },
        onGoToEqualizer = {
            try {
                activityResultLauncher.launch(
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder.player.audioSessionId)
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                )
            } catch (e: ActivityNotFoundException) {
                context.toast(strings.equalizerMissing)
            }
        },
        onShowSleepTimer = {},
        onDismiss = onDismiss
    )
}
