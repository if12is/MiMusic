package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance

/**
 * Bottom navigation with an icon and a short label for every destination.
 *
 * Items share the width equally; when a screen has more destinations than fit
 * (e.g. the settings sections) the bar scrolls horizontally and keeps the
 * selected one centered.
 */
@Composable
fun GlassyNavigationBar(
    leadingIconId: Int,
    onLeadingIconClick: () -> Unit,
    leadingIconDescription: String,
    tabIndex: Int,
    onTabIndexChanged: (Int) -> Unit,
    content: @Composable (@Composable (Int, String, Int) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    docked: Boolean = false
) {
    val (colorPalette) = LocalAppearance.current
    val systemBottom = WindowInsets.systemBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()

    val horizontalInset by animateDpAsState(
        targetValue = if (docked) 0.dp else Dimensions.glassNavigationHorizontalInset,
        animationSpec = tween(durationMillis = 220),
        label = "glassNavInset"
    )
    val bottomGap by animateDpAsState(
        targetValue = if (docked) 0.dp else Dimensions.glassNavigationBottomGap,
        animationSpec = tween(durationMillis = 220),
        label = "glassNavGap"
    )
    val shape = if (docked) {
        RectangleShape
    } else {
        RoundedCornerShape(28.dp)
    }
    val glassFill = if (docked) {
        colorPalette.background1
    } else if (colorPalette.isDark) {
        Color.Black.copy(alpha = 0.66f)
    } else {
        Color.White.copy(alpha = 0.86f)
    }
    val glassStroke = if (colorPalette.isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (docked) {
                        Modifier
                    } else {
                        Modifier.padding(systemBottom)
                    }
                )
                .padding(bottom = bottomGap)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .padding(horizontal = horizontalInset)
                    .height(Dimensions.glassNavigationBarHeight)
                    .fillMaxWidth()
                    .then(
                        if (docked) {
                            Modifier
                                .background(glassFill)
                                .drawBehind {
                                    drawLine(
                                        color = glassStroke,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                        } else {
                            Modifier
                                .shadow(
                                    elevation = 18.dp,
                                    shape = shape,
                                    ambientColor = Color.Black.copy(alpha = 0.45f),
                                    spotColor = Color.Black.copy(alpha = 0.35f)
                                )
                                .border(width = 0.6.dp, color = glassStroke, shape = shape)
                                .background(color = glassFill, shape = shape)
                                .clip(shape)
                        }
                    )
                    .padding(horizontal = 4.dp)
            ) {
                val scrollState = rememberScrollState()
                // Written during measurement, read by the auto-scroll effect below.
                val itemCount = remember { intArrayOf(1) }

                LaunchedEffect(tabIndex, scrollState.maxValue) {
                    val maxValue = scrollState.maxValue
                    if (maxValue == 0) return@LaunchedEffect
                    val count = itemCount[0].coerceAtLeast(1)
                    val contentWidth = maxValue + constraints.maxWidth
                    val item = contentWidth / count
                    // +1 skips the leading (settings/back) item.
                    val target = item * (tabIndex + 1) - (constraints.maxWidth - item) / 2
                    scrollState.animateScrollTo(target.coerceIn(0, maxValue))
                }

                val available = constraints.maxWidth
                val minItemWidth = Dimensions.glassNavigationMinItemWidth

                Layout(
                    content = {
                        GlassNavItem(
                            iconId = leadingIconId,
                            label = leadingIconDescription,
                            selected = false,
                            onClick = onLeadingIconClick
                        )

                        content { index, text, icon ->
                            GlassNavItem(
                                iconId = icon,
                                label = text,
                                selected = tabIndex == index,
                                onClick = { onTabIndexChanged(index) }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .horizontalScroll(scrollState)
                ) { measurables, layoutConstraints ->
                    val count = measurables.size.coerceAtLeast(1)
                    itemCount[0] = count
                    val itemWidth = maxOf(minItemWidth.roundToPx(), available / count)
                    val height = layoutConstraints.maxHeight
                    val placeables = measurables.map {
                        it.measure(Constraints.fixed(itemWidth, height))
                    }
                    layout(itemWidth * placeables.size, height) {
                        placeables.forEachIndexed { index, placeable ->
                            placeable.placeRelative(index * itemWidth, 0)
                        }
                    }
                }
            }
        }

        if (docked) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.systemBars)
                    .background(colorPalette.background1)
            )
        }
    }
}

@Composable
private fun GlassNavItem(
    iconId: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val contentColor by animateColorAsState(
        targetValue = if (selected) colorPalette.text else colorPalette.textSecondary,
        animationSpec = tween(durationMillis = 180),
        label = "glassNavTint"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) colorPalette.accent.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "glassNavIndicator"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 52.dp else 32.dp,
        animationSpec = tween(durationMillis = 220),
        label = "glassNavIndicatorWidth"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "glassNavScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material.ripple.rememberRipple(bounded = true),
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .padding(horizontal = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(indicatorWidth)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(indicatorColor)
        ) {
            Image(
                painter = painterResource(iconId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .size(22.dp)
            )
        }

        BasicText(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.xxs.copy(
                color = contentColor,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp)
        )
    }
}
