package it.vfsfitvnm.vimusic.utils

import java.net.URI

/**
 * HTTP is allowed only for a server the user typed that sits on the local network.
 * Public websites stay on HTTPS.
 */
object CleartextPolicy {
    fun allows(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        return when (uri.scheme?.lowercase()) {
            "https" -> true
            "http" -> isLocalHost(uri.host)
            else -> false
        }
    }

    fun isLocalHost(rawHost: String?): Boolean {
        val host = rawHost?.lowercase()?.trim()?.trim('[', ']')?.trimEnd('.').orEmpty()
        if (host.isEmpty()) return false
        if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "0.0.0.0") return true
        if (host.endsWith(".local") || host.endsWith(".lan") || host.endsWith(".home") || host.endsWith(".internal")) {
            return true
        }
        return isPrivateIpv4(host)
    }

    fun isPrivateIpv4(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        val numbers = parts.map { part -> part.toIntOrNull() ?: return false }
        if (numbers.any { it !in 0..255 }) return false
        val first = numbers[0]
        val second = numbers[1]
        return first == 10 ||
            first == 127 ||
            (first == 192 && second == 168) ||
            (first == 172 && second in 16..31) ||
            (first == 169 && second == 254)
    }
}
