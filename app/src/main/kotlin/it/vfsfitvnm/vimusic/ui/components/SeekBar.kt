package it.vfsfitvnm.vimusic.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

@Composable
fun SeekBar(
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 3.dp,
    scrubberColor: Color = color,
    scrubberRadius: Dp = 6.dp,
    shape: Shape = RectangleShape,
    drawSteps: Boolean = false,
) {
    val fraction = if (maximumValue <= minimumValue) {
        0f
    } else {
        ((value.toFloat() - minimumValue) / (maximumValue - minimumValue)).coerceIn(0f, 1f)
    }

    val isDragging = remember {
        MutableTransitionState(false)
    }

    val transition = updateTransition(transitionState = isDragging, label = null)

    val currentBarHeight by transition.animateDp(label = "") { if (it) scrubberRadius else barHeight }
    val currentScrubberRadius by transition.animateDp(label = "") { if (it) 0.dp else scrubberRadius }

    // Media progress is always left-to-right so the played segment, remaining
    // segment, and scrubber stay aligned even when the app language is RTL.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .pointerInput(minimumValue, maximumValue) {
                    if (maximumValue < minimumValue) return@pointerInput

                    var acc = 0f

                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging.targetState = true
                        },
                        onHorizontalDrag = { _, delta ->
                            acc += delta / size.width * (maximumValue - minimumValue)

                            if (acc !in -1f..1f) {
                                onDrag(acc.toLong())
                                acc -= acc.toLong()
                            }
                        },
                        onDragEnd = {
                            isDragging.targetState = false
                            acc = 0f
                            onDragEnd()
                        },
                        onDragCancel = {
                            isDragging.targetState = false
                            acc = 0f
                            onDragEnd()
                        }
                    )
                }
                .pointerInput(minimumValue, maximumValue) {
                    if (maximumValue < minimumValue) return@pointerInput

                    detectTapGestures(
                        onPress = { offset ->
                            val normalized = (offset.x / size.width).coerceIn(0f, 1f)
                            onDragStart(
                                (normalized * (maximumValue - minimumValue) + minimumValue).roundToLong()
                            )
                        },
                        onTap = {
                            onDragEnd()
                        }
                    )
                }
                .padding(horizontal = scrubberRadius)
                .height(scrubberRadius)
                .fillMaxWidth()
                .drawBehind {
                    val barHeightPx = currentBarHeight.toPx()
                    val barTop = (size.height - barHeightPx) / 2f
                    val playedWidth = size.width * fraction
                    val corner = if (shape == RectangleShape) {
                        CornerRadius.Zero
                    } else {
                        CornerRadius(barHeightPx / 2f, barHeightPx / 2f)
                    }

                    drawRoundRect(
                        color = backgroundColor,
                        topLeft = Offset(0f, barTop),
                        size = Size(size.width, barHeightPx),
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, barTop),
                        size = Size(playedWidth, barHeightPx),
                        cornerRadius = corner
                    )
                    drawCircle(
                        color = scrubberColor,
                        radius = currentScrubberRadius.toPx(),
                        center = Offset(playedWidth, size.height / 2f)
                    )

                    if (drawSteps) {
                        for (i in value + 1..maximumValue) {
                            val stepFraction =
                                (i.toFloat() - minimumValue) / (maximumValue - minimumValue)
                            drawCircle(
                                color = scrubberColor,
                                radius = scrubberRadius.toPx() / 2,
                                center = Offset(stepFraction * size.width, size.height / 2f),
                            )
                        }
                    }
                }
        )
    }
}
