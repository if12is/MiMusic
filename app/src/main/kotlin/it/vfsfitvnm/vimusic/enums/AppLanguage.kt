package it.vfsfitvnm.vimusic.enums

import java.util.Locale

enum class AppLanguage {
    Arabic,
    English;

    val code: String
        get() = when (this) {
            Arabic -> "ar"
            English -> "en"
        }

    val region: String
        get() = when (this) {
            Arabic -> "EG"
            English -> "US"
        }

    val isRtl: Boolean
        get() = this == Arabic

    val locale: Locale
        get() = Locale(code, region)

    val nativeName: String
        get() = when (this) {
            Arabic -> "العربية"
            English -> "English"
        }
}
