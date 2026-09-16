package it.vfsfitvnm.vimusic.utils

private val tashkeel = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

fun String.stripArabicMarks(): String {
    return replace(tashkeel, "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ة', 'ه')
        .replace('ى', 'ي')
        .lowercase()
}

fun String.matchesLooseArabic(query: String): Boolean {
    if (query.isBlank()) return true
    return stripArabicMarks().contains(query.stripArabicMarks())
}
