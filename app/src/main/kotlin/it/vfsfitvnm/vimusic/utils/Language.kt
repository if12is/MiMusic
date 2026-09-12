package it.vfsfitvnm.vimusic.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import it.vfsfitvnm.innertube.models.Context as InnertubeContext
import it.vfsfitvnm.vimusic.enums.AppLanguage
import it.vfsfitvnm.vimusic.ui.styling.UiStrings
import java.util.Locale

const val appLanguageKey = "appLanguage"

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.Arabic }

val LocalStrings = staticCompositionLocalOf { UiStrings(AppLanguage.Arabic) }

fun Context.preferredAppLanguage(): AppLanguage =
    preferences.getEnum(appLanguageKey, AppLanguage.Arabic)

fun applyInnertubeLocale(language: AppLanguage) {
    InnertubeContext.hl = language.code
    InnertubeContext.gl = language.region
}

fun Context.withAppLanguage(language: AppLanguage = preferredAppLanguage()): Context {
    applyInnertubeLocale(language)
    Locale.setDefault(language.locale)

    val configuration = Configuration(resources.configuration)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.setLocale(language.locale)
        configuration.setLayoutDirection(language.locale)
    } else {
        @Suppress("DEPRECATION")
        configuration.locale = language.locale
        configuration.setLayoutDirection(language.locale)
    }

    return createConfigurationContext(configuration)
}
