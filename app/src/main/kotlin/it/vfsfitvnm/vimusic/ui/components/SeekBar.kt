package it.vfsfitvnm.vimusic.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val fraction = if (maximumValue < minimumValue) {
        0f
    } else {
        (value.toFloat() - minimumValue) / (maximumValue - minimumValue)
    }

    val isDragging = remember {
        MutableTransitionState(false)
    }

    val transition = updateTransition(transitionState = isDragging, label = null)

    val currentBarHeight by transition.animateDp(label = "") { if (it) scrubberRadius else barHeight }
    val currentScrubberRadius by transition.animateDp(label = "") { if (it) 0.dp else scrubberRadius }

    Box(
        modifier = modifier
            .pointerInput(minimumValue, maximumValue, isRtl) {
                if (maximumValue < minimumValue) return@pointerInput

                var acc = 0f

                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging.targetState = true
                    },
                    onHorizontalDrag = { _, delta ->
                        val directedDelta = if (isRtl) -delta else delta
                        acc += directedDelta / size.width * (maximumValue - minimumValue)

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
            .pointerInput(minimumValue, maximumValue, isRtl) {
                if (maximumValue < minimumValue) return@pointerInput

                detectTapGestures(
                    onPress = { offset ->
                        val fraction = offset.x / size.width
                        val normalized = if (isRtl) 1f - fraction else fraction
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
            .drawWithContent {
                drawContent()

                val scrubberPosition = if (isRtl) {
                    (1f - fraction) * size.width
                } else {
                    fraction * size.width
                }

                drawCircle(
                    color = scrubberColor,
                    radius = currentScrubberRadius.toPx(),
                    center = center.copy(x = scrubberPosition)
                )

                if (drawSteps) {
                    for (i in value + 1..maximumValue) {
                        val stepFraction = (i.toFloat() - minimumValue) / (maximumValue - minimumValue)
                        val stepPosition = if (isRtl) {
                            (1f - stepFraction) * size.width
                        } else {
                            stepFraction * size.width
                        }
                        drawCircle(
                            color = scrubberColor,
                            radius = scrubberRadius.toPx() / 2,
                            center = center.copy(x = stepPosition),
                        )
                    }
                }
            }
            .height(scrubberRadius)
    ) {
        Spacer(
            modifier = Modifier
                .height(currentBarHeight)
                .fillMaxWidth()
                .background(color = backgroundColor, shape = shape)
                .align(Alignment.Center)
        )

        Spacer(
            modifier = Modifier
                .height(currentBarHeight)
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(color = color, shape = shape)
                .align(if (isRtl) Alignment.CenterEnd else Alignment.CenterStart)
        )
    }
}
