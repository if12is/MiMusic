package it.vfsfitvnm.vimusic.utils

import java.util.Calendar

data class HomeMood(
    val label: String,
    val query: String
)

fun orderedHomeMoods(
    labels: List<HomeMood>,
    calendar: Calendar = Calendar.getInstance()
): List<HomeMood> {
    if (labels.isEmpty()) return labels
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val friday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    val byQuery = labels.associateBy { it.query }

    val preferredQueries = when {
        friday -> listOf(
            "تلاوة قرآن",
            "موسيقى هادئة",
            "طرب عربي",
            "موسيقى للعمل",
            "أغاني حماسية",
            "شعبي مصري"
        )
        hour in 5 until 11 -> listOf(
            "موسيقى هادئة",
            "موسيقى للعمل",
            "تلاوة قرآن",
            "طرب عربي",
            "أغاني حماسية",
            "شعبي مصري"
        )
        hour in 18 until 24 || hour < 5 -> listOf(
            "طرب عربي",
            "موسيقى هادئة",
            "تلاوة قرآن",
            "شعبي مصري",
            "أغاني حماسية",
            "موسيقى للعمل"
        )
        else -> listOf(
            "أغاني حماسية",
            "شعبي مصري",
            "طرب عربي",
            "موسيقى للعمل",
            "موسيقى هادئة",
            "تلاوة قرآن"
        )
    }

    val ordered = preferredQueries.mapNotNull(byQuery::get)
    return ordered + labels.filterNot { mood -> ordered.any { it.query == mood.query } }
}

fun isLikelyQuran(title: String?, artist: String?): Boolean {
    val haystack = "${title.orEmpty()} ${artist.orEmpty()}"
    return haystack.contains("قرآن") ||
        haystack.contains("تلاوة") ||
        haystack.contains("سورة") ||
        haystack.contains("Quran", ignoreCase = true)
}
