package it.vfsfitvnm.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Thumbnail(
    val url: String,
    val height: Int?,
    val width: Int?
) {
    val isResizable: Boolean
        get() = !url.startsWith("https://i.ytimg.com")

    fun size(size: Int): String {
        return when {
            url.startsWith("https://lh3.googleusercontent.com") -> "$url-w$size-h$size"
            url.startsWith("https://yt3.ggpht.com") -> "$url-s$size"
            url.contains("i.ytimg.com/vi/") || url.contains("img.youtube.com/vi/") -> {
                val videoId = Regex("""(?:i\.ytimg\.com|img\.youtube\.com)/vi/([^/]+)/""")
                    .find(url)
                    ?.groupValues
                    ?.get(1)
                    ?: return url
                when {
                    size >= 480 -> "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
                    size >= 240 -> "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    else -> "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
                }
            }
            else -> url
        }
    }
}
