package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance

@Composable
fun GlassyNavigationBar(
    leadingIconId: Int,
    onLeadingIconClick: () -> Unit,
    leadingIconDescription: String,
    tabIndex: Int,
    onTabIndexChanged: (Int) -> Unit,
    content: @Composable RowScope.(@Composable (Int, String, Int) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val systemBottom = WindowInsets.systemBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()

    val capsule = RoundedCornerShape(percent = 50)
    val glassFill = if (colorPalette.isDark) {
        Color.Black.copy(alpha = 0.58f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }
    val glassStroke = if (colorPalette.isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .fillMaxWidth()
            .padding(systemBottom)
            .padding(bottom = Dimensions.glassNavigationBottomGap)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .padding(horizontal = Dimensions.glassNavigationHorizontalInset)
                .height(Dimensions.glassNavigationBarHeight)
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = capsule,
                    ambientColor = Color.Black.copy(alpha = 0.45f),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .border(width = 0.6.dp, color = glassStroke, shape = capsule)
                .background(color = glassFill, shape = capsule)
                .clip(capsule)
                .padding(horizontal = 6.dp)
        ) {
            GlassNavIcon(
                iconId = leadingIconId,
                contentDescription = leadingIconDescription,
                selected = false,
                onClick = onLeadingIconClick
            )

            content { index, text, icon ->
                GlassNavIcon(
                    iconId = icon,
                    contentDescription = text,
                    selected = tabIndex == index,
                    onClick = { onTabIndexChanged(index) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.GlassNavIcon(
    iconId: Int,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val tint by animateColorAsState(
        targetValue = if (selected) colorPalette.text else colorPalette.textDisabled,
        animationSpec = tween(durationMillis = 180),
        label = "glassNavTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "glassNavScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .height(Dimensions.glassNavigationBarHeight)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .size(24.dp)
        )
    }
}
