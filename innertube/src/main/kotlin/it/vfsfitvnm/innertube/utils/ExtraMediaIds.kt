package it.vfsfitvnm.innertube.utils

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ExtraMediaIds {
    const val SOUNDCLOUD_PREFIX = "sc:"
    const val PODCAST_PREFIX = "podcast:"
    const val JELLYFIN_PREFIX = "jellyfin:"

    fun isExternal(id: String): Boolean {
        return id.startsWith(SOUNDCLOUD_PREFIX) ||
            id.startsWith(PODCAST_PREFIX) ||
            id.startsWith(JELLYFIN_PREFIX)
    }

    fun encode(prefix: String, value: String): String {
        return prefix + URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    }

    fun soundCloud(pageUrl: String): String = encode(SOUNDCLOUD_PREFIX, pageUrl)

    fun podcast(enclosureUrl: String): String = encode(PODCAST_PREFIX, enclosureUrl)

    fun jellyfin(itemId: String): String = encode(JELLYFIN_PREFIX, itemId)

    fun decode(mediaId: String): Pair<String, String>? {
        val prefix = listOf(SOUNDCLOUD_PREFIX, PODCAST_PREFIX, JELLYFIN_PREFIX)
            .firstOrNull { mediaId.startsWith(it) }
            ?: return null
        val encoded = mediaId.removePrefix(prefix)
        val value = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
        return prefix to value
    }

    fun payload(mediaId: String): String? = decode(mediaId)?.second
}
