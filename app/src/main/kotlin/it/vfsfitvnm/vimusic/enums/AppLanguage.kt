package it.vfsfitvnm.vimusic.enums

import java.util.Locale

enum class AppLanguage {
    Arabic,
    English,
    French,
    Turkish,
    Urdu;

    val code: String
        get() = when (this) {
            Arabic -> "ar"
            English -> "en"
            French -> "fr"
            Turkish -> "tr"
            Urdu -> "ur"
        }

    val region: String
        get() = when (this) {
            Arabic -> "EG"
            English -> "US"
            French -> "FR"
            Turkish -> "TR"
            Urdu -> "PK"
        }

    val isRtl: Boolean
        get() = this == Arabic || this == Urdu

    val locale: Locale
        get() = Locale(code, region)

    val nativeName: String
        get() = when (this) {
            Arabic -> "العربية"
            English -> "English"
            French -> "Français"
            Turkish -> "Türkçe"
            Urdu -> "اردو"
        }

    companion object {
        val translated = listOf(Arabic, English)
    }
}
