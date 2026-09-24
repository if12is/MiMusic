package it.vfsfitvnm.vimusic.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.components.SeekBar
import it.vfsfitvnm.vimusic.ui.components.themed.IconButton
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.favoritesIcon
import it.vfsfitvnm.vimusic.utils.bold
import it.vfsfitvnm.vimusic.utils.forceSeekToNext
import it.vfsfitvnm.vimusic.utils.forceSeekToPrevious
import it.vfsfitvnm.vimusic.utils.shuffleQueue
import it.vfsfitvnm.vimusic.utils.formatAsDuration
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.smartShuffleKey
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.carModeKey
import it.vfsfitvnm.vimusic.utils.playbackSpeedKey
import it.vfsfitvnm.vimusic.utils.trackLoopEnabledKey
import it.vfsfitvnm.vimusic.utils.videoModeKey
import it.vfsfitvnm.vimusic.utils.LocalStrings
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun Controls(
    mediaId: String,
    title: String?,
    artist: String?,
    shouldBePlaying: Boolean,
    position: Long,
    duration: Long,
    onShowLyrics: () -> Unit = {},
    onShowSleepTimer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current

    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return
    var trackLoopEnabled by rememberPreference(trackLoopEnabledKey, defaultValue = false)
    var playbackSpeed by rememberPreference(playbackSpeedKey, 1f)
    val carMode by rememberPreference(carModeKey, false)
    val smartShuffle by rememberPreference(smartShuffleKey, true)
    val strings = LocalStrings.current
    val speedOptions = listOf(0.75f, 1f, 1.25f, 1.5f)
    val videoMode by rememberPreference(videoModeKey, false)
    val hasVideo = binder.hasVideo(mediaId) || binder.isPlayingVideo(mediaId)

    var scrubbingPosition by remember(mediaId) {
        mutableStateOf<Long?>(null)
    }

    var likedAt by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(mediaId) {
        Database.likedAt(mediaId).distinctUntilChanged().collect { likedAt = it }
    }

    val skipGlyph = if (carMode) 40.dp else 34.dp
    val playButtonSize = if (carMode) 72.dp else 56.dp
    val sideGlyph = if (carMode) 34.dp else 28.dp
    val sideIconSize = if (carMode) 44.dp else 40.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
        )

        // 0. Song / Video switch, like YouTube Music; only when a real video exists.
        AnimatedVisibility(
            visible = hasVideo,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                AudioVideoSwitch(
                    videoMode = videoMode,
                    onChange = binder::setVideoMode
                )
                if (!videoMode) {
                    BasicText(
                        text = strings.videoAvailableHint,
                        style = typography.xxs.semiBold.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // 1. What is playing + like, the most common action on a song.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = title ?: "",
                    style = typography.l.bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                BasicText(
                    text = artist ?: "",
                    style = typography.s.semiBold.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                icon = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                color = colorPalette.favoritesIcon,
                onClick = {
                    val currentMediaItem = binder.player.currentMediaItem
                    query {
                        if (Database.like(
                                mediaId,
                                if (likedAt == null) System.currentTimeMillis() else null
                            ) == 0
                        ) {
                            currentMediaItem
                                ?.takeIf { it.mediaId == mediaId }
                                ?.let {
                                    Database.insert(currentMediaItem, Song::toggleLike)
                                }
                        }
                    }
                },
                contentDescription = strings.likeSong,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(sideIconSize)
            )
        }

        Spacer(
            modifier = Modifier
                .height(20.dp)
        )

        // 2. Progress.
        SeekBar(
            value = scrubbingPosition ?: position,
            minimumValue = 0,
            maximumValue = duration,
            onDragStart = {
                scrubbingPosition = it
            },
            onDrag = { delta ->
                scrubbingPosition = if (duration != C.TIME_UNSET) {
                    scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                } else {
                    null
                }
            },
            onDragEnd = {
                scrubbingPosition?.let(binder.player::seekTo)
                scrubbingPosition = null
            },
            color = colorPalette.text,
            backgroundColor = colorPalette.background2,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(
            modifier = Modifier
                .height(8.dp)
        )

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                BasicText(
                    text = formatAsDuration(scrubbingPosition ?: position),
                    style = typography.xxs.semiBold.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (duration != C.TIME_UNSET) {
                    BasicText(
                        text = formatAsDuration(duration),
                        style = typography.xxs.semiBold.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier
                .weight(0.6f)
        )

        // 3. Transport: symmetric, with the play button as the clear focal point.
        //    Media controls keep LTR order in RTL too (previous on the left).
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                LabeledTransportButton(
                    icon = R.drawable.infinite,
                    label = strings.repeatShort,
                    color = if (trackLoopEnabled) colorPalette.accent else colorPalette.textSecondary,
                    glyph = sideGlyph,
                    onClick = { trackLoopEnabled = !trackLoopEnabled }
                )

                LabeledTransportButton(
                    icon = R.drawable.play_skip_back,
                    label = strings.previousSong,
                    color = colorPalette.text,
                    glyph = skipGlyph,
                    onClick = binder.player::forceSeekToPrevious
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colorPalette.text)
                            .clickable(role = Role.Button) {
                                if (shouldBePlaying) {
                                    binder.player.pause()
                                } else {
                                    if (binder.player.playbackState == Player.STATE_IDLE) {
                                        binder.player.prepare()
                                    }
                                    binder.player.play()
                                }
                            }
                            .size(playButtonSize)
                    ) {
                        Image(
                            painter = painterResource(if (shouldBePlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = if (shouldBePlaying) strings.pause else strings.play,
                            colorFilter = ColorFilter.tint(colorPalette.background0),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(if (carMode) 28.dp else 22.dp)
                        )
                    }
                    BasicText(
                        text = if (shouldBePlaying) strings.pauseShort else strings.play,
                        style = typography.xxs.semiBold.copy(
                            color = colorPalette.textSecondary,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                LabeledTransportButton(
                    icon = R.drawable.play_skip_forward,
                    label = strings.nextSong,
                    color = colorPalette.text,
                    glyph = skipGlyph,
                    onClick = binder.player::forceSeekToNext
                )

                LabeledTransportButton(
                    icon = R.drawable.shuffle,
                    label = strings.shuffle,
                    color = colorPalette.textSecondary,
                    glyph = sideGlyph,
                    onClick = { binder.player.shuffleQueue(smartShuffle) }
                )
            }
        }

        Spacer(
            modifier = Modifier
                .weight(0.6f)
        )

        // 4. Secondary tools: one evenly spaced row, every icon labelled.
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            PlayerToolButton(
                label = strings.lyrics,
                icon = R.drawable.text,
                onClick = onShowLyrics,
                modifier = Modifier.weight(1f)
            )

            PlayerToolButton(
                label = strings.speedShort,
                badge = "${playbackSpeed}×",
                active = playbackSpeed != 1f,
                onClick = {
                    val index = speedOptions.indexOfFirst { it == playbackSpeed }
                    playbackSpeed = speedOptions[(index + 1).mod(speedOptions.size)]
                    binder.setPlaybackSpeed(playbackSpeed)
                },
                modifier = Modifier.weight(1f)
            )

            PlayerToolButton(
                label = strings.sleepTimerShort,
                icon = R.drawable.alarm,
                onClick = onShowSleepTimer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(
            modifier = Modifier
                .weight(1f)
        )
    }
}

@Composable
private fun LabeledTransportButton(
    icon: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    glyph: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val (_, typography) = LocalAppearance.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier.size(glyph)
        )
        BasicText(
            text = label,
            style = typography.xxs.semiBold.copy(color = color, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PlayerToolButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    badge: String? = null,
    active: Boolean = false
) {
    val (colorPalette, typography) = LocalAppearance.current
    val contentColor = if (active) colorPalette.accent else colorPalette.text

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
            .sizeIn(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp)
        ) {
            if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier.size(20.dp)
                )
            } else if (badge != null) {
                BasicText(
                    text = badge,
                    maxLines = 1,
                    style = typography.xs.bold.copy(color = contentColor, fontSize = 13.sp)
                )
            }
        }

        BasicText(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.xxs.semiBold.copy(
                color = if (active) colorPalette.accent else colorPalette.textSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun AudioVideoSwitch(
    videoMode: Boolean,
    onChange: (Boolean) -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val strings = LocalStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colorPalette.background2)
            .padding(3.dp)
    ) {
        listOf(false to strings.modeAudio, true to strings.modeVideo).forEach { (isVideo, label) ->
            val selected = videoMode == isVideo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) colorPalette.text else colorPalette.background2)
                    .clickable(role = Role.Tab) { if (!selected) onChange(isVideo) }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Image(
                    painter = painterResource(if (isVideo) R.drawable.film else R.drawable.musical_notes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if (selected) colorPalette.background0 else colorPalette.textSecondary
                    ),
                    modifier = Modifier.size(14.dp)
                )
                BasicText(
                    text = label,
                    style = typography.xxs.semiBold.copy(
                        color = if (selected) colorPalette.background0 else colorPalette.textSecondary
                    ),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
