package it.vfsfitvnm.vimusic.ui.screens.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.enums.AppFont
import it.vfsfitvnm.vimusic.enums.AppLanguage
import it.vfsfitvnm.vimusic.enums.ColorPaletteMode
import it.vfsfitvnm.vimusic.enums.ColorPaletteName
import it.vfsfitvnm.vimusic.enums.NavigationStyle
import it.vfsfitvnm.vimusic.enums.ThumbnailRoundness
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.appFontKey
import it.vfsfitvnm.vimusic.utils.appLanguageKey
import it.vfsfitvnm.vimusic.utils.applyFontPaddingKey
import it.vfsfitvnm.vimusic.utils.colorPaletteModeKey
import it.vfsfitvnm.vimusic.utils.colorPaletteNameKey
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid13
import it.vfsfitvnm.vimusic.utils.isShowingThumbnailInLockscreenKey
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.navigationStyleKey
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.selectedAppFont
import it.vfsfitvnm.vimusic.utils.thumbnailRoundnessKey
import it.vfsfitvnm.vimusic.utils.lyricsScaleKey
import it.vfsfitvnm.vimusic.utils.lowPowerModeKey

@ExperimentalAnimationApi
@Composable
fun AppearanceSettings() {
    val (colorPalette) = LocalAppearance.current
    val strings = LocalStrings.current
    val context = LocalContext.current

    var appLanguage by rememberPreference(appLanguageKey, AppLanguage.Arabic)
    var colorPaletteName by rememberPreference(colorPaletteNameKey, ColorPaletteName.Dynamic)
    var colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.System)
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Light
    )
    var navigationStyle by rememberPreference(
        navigationStyleKey,
        NavigationStyle.Side
    )
    var appFont by rememberPreference(appFontKey, context.preferences.selectedAppFont())
    var applyFontPadding by rememberPreference(applyFontPaddingKey, false)
    var lowPowerMode by rememberPreference(lowPowerModeKey, false)
    var lyricsScale by rememberPreference(lyricsScaleKey, 1)
    var isShowingThumbnailInLockscreen by rememberPreference(
        isShowingThumbnailInLockscreenKey,
        false
    )

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        Header(title = strings.appearance)

        SettingsEntryGroupText(title = strings.languageGroup)

        EnumValueSelectorSettingsEntry(
            title = strings.languageTitle,
            selectedValue = appLanguage,
            onValueSelected = { appLanguage = it },
            valueText = { it.nativeName }
        )

        SettingsDescription(text = strings.languageDescription)

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.colors)

        EnumValueSelectorSettingsEntry(
            title = strings.theme,
            selectedValue = colorPaletteName,
            onValueSelected = { colorPaletteName = it },
            valueText = strings::colorPaletteName
        )

        EnumValueSelectorSettingsEntry(
            title = strings.themeMode,
            selectedValue = colorPaletteMode,
            isEnabled = colorPaletteName != ColorPaletteName.PureBlack &&
                colorPaletteName != ColorPaletteName.Gold &&
                colorPaletteName != ColorPaletteName.Spotify &&
                colorPaletteName != ColorPaletteName.YouTube,
            onValueSelected = { colorPaletteMode = it },
            valueText = strings::colorPaletteMode
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.navigationGroup)

        EnumValueSelectorSettingsEntry(
            title = strings.navigationStyle,
            selectedValue = navigationStyle,
            onValueSelected = { navigationStyle = it },
            valueText = strings::navigationStyleName
        )

        SettingsDescription(text = strings.navigationStyleDescription)

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.shapes)

        EnumValueSelectorSettingsEntry(
            title = strings.thumbnailRoundness,
            selectedValue = thumbnailRoundness,
            onValueSelected = { thumbnailRoundness = it },
            valueText = strings::thumbnailRoundnessName,
            trailingContent = {
                Spacer(
                    modifier = Modifier
                        .border(width = 1.dp, color = colorPalette.accent,  shape = thumbnailRoundness.shape())
                        .background(color = colorPalette.background1, shape = thumbnailRoundness.shape())
                        .size(36.dp)
                )
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.textGroup)

        EnumValueSelectorSettingsEntry(
            title = strings.appFont,
            selectedValue = appFont,
            onValueSelected = { appFont = it },
            valueText = strings::appFontName
        )

        SettingsDescription(text = strings.appFontDescription)

        SwitchSettingEntry(
            title = strings.applyFontPadding,
            text = strings.applyFontPaddingDescription,
            isChecked = applyFontPadding,
            onCheckedChange = { applyFontPadding = it }
        )

        SwitchSettingEntry(
            title = strings.lowPowerMode,
            text = strings.lowPowerModeDescription,
            isChecked = lowPowerMode,
            onCheckedChange = { lowPowerMode = it }
        )

        ValueSelectorSettingsEntry(
            title = strings.lyricsSize,
            selectedValue = lyricsScale.coerceIn(0, 2),
            values = listOf(0, 1, 2),
            onValueSelected = { lyricsScale = it },
            valueText = {
                when (it) {
                    0 -> strings.roundnessLight
                    2 -> strings.roundnessHeavy
                    else -> strings.roundnessMedium
                }
            }
        )

        if (!isAtLeastAndroid13) {
            SettingsGroupSpacer()

            SettingsEntryGroupText(title = strings.lockscreen)

            SwitchSettingEntry(
                title = strings.showSongCover,
                text = strings.showSongCoverDescription,
                isChecked = isShowingThumbnailInLockscreen,
                onCheckedChange = { isShowingThumbnailInLockscreen = it }
            )
        }
    }
}
