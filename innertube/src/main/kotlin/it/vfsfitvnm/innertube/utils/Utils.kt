package it.vfsfitvnm.innertube.utils

import io.ktor.utils.io.CancellationException
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.SectionListRenderer

internal fun SectionListRenderer.findSectionByTitle(vararg texts: String): SectionListRenderer.Content? {
    val aliases = texts.filter { it.isNotBlank() }
    if (aliases.isEmpty()) return null

    return contents?.find { content ->
        val title = content
            .musicCarouselShelfRenderer
            ?.header
            ?.musicCarouselShelfBasicHeaderRenderer
            ?.title
            ?: content
                .musicShelfRenderer
                ?.title

        matchesAnyAlias(title?.runs?.firstOrNull()?.text, aliases)
    }
}

internal fun SectionListRenderer.findSectionByStrapline(vararg texts: String): SectionListRenderer.Content? {
    val aliases = texts.filter { it.isNotBlank() }
    if (aliases.isEmpty()) return null

    return contents?.find { content ->
        val strapline = content
            .musicCarouselShelfRenderer
            ?.header
            ?.musicCarouselShelfBasicHeaderRenderer
            ?.strapline
            ?.runs
            ?.firstOrNull()
            ?.text

        matchesAnyAlias(strapline, aliases)
    }
}

internal fun matchesAnyAlias(value: String?, aliases: Collection<String>): Boolean {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return false

    return aliases.any { alias ->
        text.equals(alias, ignoreCase = true) || text.contains(alias, ignoreCase = true)
    }
}

internal inline fun <R> runCatchingNonCancellable(block: () -> R): Result<R>? {
    val result = runCatching(block)
    return when (result.exceptionOrNull()) {
        is CancellationException -> null
        else -> result
    }
}

infix operator fun <T : Innertube.Item> Innertube.ItemsPage<T>?.plus(other: Innertube.ItemsPage<T>) =
    other.copy(
        items = (this?.items?.plus(other.items ?: emptyList())
            ?: other.items)?.distinctBy(Innertube.Item::key)
    )
