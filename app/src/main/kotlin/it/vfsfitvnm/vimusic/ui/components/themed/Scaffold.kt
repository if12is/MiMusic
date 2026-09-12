package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import it.vfsfitvnm.vimusic.enums.NavigationStyle
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.navigationStyleKey
import it.vfsfitvnm.vimusic.utils.rememberPreference

@ExperimentalAnimationApi
@Composable
fun Scaffold(
    topIconButtonId: Int,
    onTopIconButtonClick: () -> Unit,
    tabIndex: Int,
    onTabChanged: (Int) -> Unit,
    tabColumnContent: @Composable (@Composable (Int, String, Int) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val strings = LocalStrings.current
    val navigationStyle by rememberPreference(navigationStyleKey, NavigationStyle.Side)

    if (navigationStyle == NavigationStyle.GlassBottom) {
        Box(
            modifier = modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = tabIndex,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val slideDirection = when (targetState > initialState) {
                        true -> AnimatedContentTransitionScope.SlideDirection.Start
                        false -> AnimatedContentTransitionScope.SlideDirection.End
                    }

                    val animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )

                    slideIntoContainer(slideDirection, animationSpec) togetherWith
                        slideOutOfContainer(slideDirection, animationSpec)
                },
                content = content
            )

            GlassyNavigationBar(
                leadingIconId = topIconButtonId,
                onLeadingIconClick = onTopIconButtonClick,
                leadingIconDescription = strings.navigationAction,
                tabIndex = tabIndex,
                onTabIndexChanged = onTabChanged,
                content = { item ->
                    tabColumnContent(item)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    } else {
        Row(
            modifier = modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            NavigationRail(
                topIconButtonId = topIconButtonId,
                onTopIconButtonClick = onTopIconButtonClick,
                tabIndex = tabIndex,
                onTabIndexChanged = onTabChanged,
                content = { item ->
                    tabColumnContent(item)
                }
            )

            AnimatedContent(
                targetState = tabIndex,
                transitionSpec = {
                    val slideDirection = when (targetState > initialState) {
                        true -> AnimatedContentTransitionScope.SlideDirection.Up
                        false -> AnimatedContentTransitionScope.SlideDirection.Down
                    }

                    val animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )

                    slideIntoContainer(slideDirection, animationSpec) togetherWith
                        slideOutOfContainer(slideDirection, animationSpec)
                },
                content = content
            )
        }
    }
}
