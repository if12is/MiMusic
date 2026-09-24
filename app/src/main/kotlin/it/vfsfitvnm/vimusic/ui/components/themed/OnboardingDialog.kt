package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.enums.AppLanguage
import it.vfsfitvnm.vimusic.enums.NavigationStyle
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.Region
import it.vfsfitvnm.vimusic.utils.appLanguageKey
import it.vfsfitvnm.vimusic.utils.contentRegionKey
import it.vfsfitvnm.vimusic.utils.displayed
import it.vfsfitvnm.vimusic.utils.navigationStyleKey
import it.vfsfitvnm.vimusic.utils.onboardingStepKey
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.semiBold
import java.util.Locale

@Composable
fun OnboardingDialog(onFinished: () -> Unit) {
    val context = LocalContext.current
    val (_, typography) = LocalAppearance.current
    val strings = LocalStrings.current
    var step by rememberPreference(onboardingStepKey, 0)
    var language by rememberPreference(appLanguageKey, AppLanguage.Arabic)
    var navigationStyle by rememberPreference(navigationStyleKey, NavigationStyle.Side)
    var region by rememberPreference(contentRegionKey, "auto")
    val shownLanguage = language.displayed()

    val title = when (step) {
        0 -> strings.onboardingLanguage
        1 -> strings.onboardingNavigation
        else -> strings.onboardingRegion
    }

    DefaultDialog(onDismiss = { }) {
        BasicText(
            text = strings.onboardingTitle,
            style = typography.s.semiBold,
            modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp)
        )
        BasicText(
            text = title,
            style = typography.xs.secondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            when (step) {
                0 -> AppLanguage.translated.forEach { choice ->
                    ChoiceRow(
                        label = choice.nativeName,
                        selected = choice == shownLanguage,
                        onClick = { language = choice }
                    )
                }
                1 -> NavigationStyle.entries.forEach { choice ->
                    ChoiceRow(
                        label = strings.navigationStyleName(choice),
                        selected = choice == navigationStyle,
                        onClick = { navigationStyle = choice }
                    )
                }
                else -> {
                    ChoiceRow(
                        label = strings.contentRegionAuto,
                        selected = region == "auto",
                        onClick = {
                            region = "auto"
                            Region.useAutomatic(context)
                        }
                    )
                    BasicText(
                        text = strings.packageMoveDescription,
                        style = typography.xxs.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Region.selectableRegions.forEach { code ->
                        ChoiceRow(
                            label = Region.displayName(
                                code,
                                if (shownLanguage == AppLanguage.Arabic) Locale("ar") else Locale.ENGLISH
                            ),
                            selected = region.equals(code, ignoreCase = true),
                            onClick = {
                                region = code
                                Region.useManual(context, code)
                            }
                        )
                    }
                }
            }
        }

        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            if (step > 0) {
                DialogTextButton(
                    text = strings.onboardingBack,
                    onClick = { step -= 1 }
                )
            }
            DialogTextButton(
                text = if (step >= 2) strings.onboardingStart else strings.onboardingNext,
                primary = true,
                onClick = {
                    if (step >= 2) {
                        step = 0
                        onFinished()
                    } else {
                        step += 1
                    }
                }
            )
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val (colorPalette, typography) = LocalAppearance.current
    BasicText(
        text = if (selected) "✓  $label" else label,
        style = typography.xs.semiBold.copy(
            color = if (selected) colorPalette.accent else colorPalette.text
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}
