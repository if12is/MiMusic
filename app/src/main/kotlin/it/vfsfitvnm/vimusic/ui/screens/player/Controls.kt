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
import it.vfsfitvnm.vimusic.utils.playbackPitchKey
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
    var playbackPitch by rememberPreference(playbackPitchKey, 1f)
    val carMode by rememberPreference(carModeKey, false)
    val smartShuffle by rememberPreference(smartShuffleKey, true)
    val strings = LocalStrings.current
    val speedOptions = listOf(0.75f, 1f, 1.25f, 1.5f)
    val pitchOptions = listOf(0.8f, 0.9f, 1f, 1.1f, 1.2f)
    var loopA by remember(mediaId) { mutableStateOf<Long?>(null) }
    var loopB by remember(mediaId) { mutableStateOf<Long?>(null) }
    var showMoreTools by rememberSaveable { mutableStateOf(false) }
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

    val skipIconSize = if (carMode) 56.dp else 48.dp
    val playButtonSize = if (carMode) 84.dp else 68.dp
    val sideIconSize = 48.dp

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
                IconButton(
                    icon = R.drawable.infinite,
                    color = if (trackLoopEnabled) colorPalette.accent else colorPalette.textSecondary,
                    onClick = { trackLoopEnabled = !trackLoopEnabled },
                    contentDescription = strings.repeatSong,
                    modifier = Modifier.size(sideIconSize)
                )

                IconButton(
                    icon = R.drawable.play_skip_back,
                    color = colorPalette.text,
                    onClick = binder.player::forceSeekToPrevious,
                    contentDescription = strings.previousSong,
                    modifier = Modifier.size(skipIconSize)
                )

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
                            .size(if (carMode) 34.dp else 28.dp)
                    )
                }

                IconButton(
                    icon = R.drawable.play_skip_forward,
                    color = colorPalette.text,
                    onClick = binder.player::forceSeekToNext,
                    contentDescription = strings.nextSong,
                    modifier = Modifier.size(skipIconSize)
                )

                IconButton(
                    icon = R.drawable.shuffle,
                    color = colorPalette.textSecondary,
                    onClick = { binder.player.shuffleQueue(smartShuffle) },
                    contentDescription = strings.shuffle,
                    modifier = Modifier.size(sideIconSize)
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

            PlayerToolButton(
                label = strings.moreTools,
                icon = if (showMoreTools) R.drawable.chevron_up else R.drawable.ellipsis_horizontal,
                active = showMoreTools || playbackPitch != 1f || loopA != null || loopB != null,
                onClick = { showMoreTools = !showMoreTools },
                modifier = Modifier.weight(1f)
            )
        }

        // 5. Advanced tools stay out of the way until asked for.
        AnimatedVisibility(
            visible = showMoreTools,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                PlayerChip(
                    text = "${strings.pitch} ${playbackPitch}",
                    active = playbackPitch != 1f,
                    onClick = {
                        val index = pitchOptions.indexOfFirst { it == playbackPitch }
                        playbackPitch = pitchOptions[(index + 1).mod(pitchOptions.size)]
                        binder.setPlaybackPitch(playbackPitch)
                    }
                )
                PlayerChip(
                    text = loopA?.let { "A ${formatAsDuration(it)}" } ?: strings.markA,
                    active = loopA != null,
                    onClick = {
                        loopA = binder.player.currentPosition
                        loopB?.let { end ->
                            loopA?.let { start -> binder.setAbLoop(start, end) }
                        }
                    }
                )
                PlayerChip(
                    text = loopB?.let { "B ${formatAsDuration(it)}" } ?: strings.markB,
                    active = loopB != null,
                    onClick = {
                        loopB = binder.player.currentPosition
                        loopA?.let { start ->
                            loopB?.let { end -> binder.setAbLoop(start, end) }
                        }
                    }
                )
                if (loopA != null || loopB != null) {
                    PlayerChip(
                        text = strings.clearLoop,
                        active = false,
                        onClick = {
                            loopA = null
                            loopB = null
                            binder.clearAbLoop()
                        }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier
                .weight(1f)
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
private fun PlayerChip(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = text,
        maxLines = 1,
        style = typography.xxs.semiBold.copy(
            color = if (active) colorPalette.accent else colorPalette.text
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (active) colorPalette.accent.copy(alpha = 0.16f) else colorPalette.background2
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
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
