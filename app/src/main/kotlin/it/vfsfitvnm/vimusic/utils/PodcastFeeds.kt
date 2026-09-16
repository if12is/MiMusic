package it.vfsfitvnm.vimusic.utils

import android.util.Xml
import it.vfsfitvnm.innertube.utils.ExtraMediaIds
import it.vfsfitvnm.innertube.utils.ExtraTrack
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

const val podcastFeedsKey = "podcastFeedUrls"

fun parsePodcastFeedUrls(raw: String): List<String> {
    return raw.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("http://") || it.startsWith("https://") }
        .distinct()
        .toList()
}

fun fetchPodcastEpisodes(feedUrls: List<String>, query: String = "", limit: Int = 40): List<ExtraTrack> {
    val needle = query.trim()
    return feedUrls.flatMap { url ->
        runCatching { parseRss(httpGet(url), url) }.getOrDefault(emptyList())
    }.filter { episode ->
        needle.isEmpty() ||
            episode.title.contains(needle, ignoreCase = true) ||
            episode.artist.orEmpty().contains(needle, ignoreCase = true)
    }.take(limit)
}

private fun parseRss(xml: String, feedUrl: String): List<ExtraTrack> {
    val parser = Xml.newPullParser().apply {
        setInput(StringReader(xml))
    }
    var event = parser.eventType
    var inItem = false
    var title: String? = null
    var artist: String? = null
    var enclosure: String? = null
    var duration: String? = null
    var image: String? = null
    val episodes = mutableListOf<ExtraTrack>()

    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                val name = parser.name.substringAfter(':').lowercase()
                when (name) {
                    "item" -> {
                        inItem = true
                        title = null
                        artist = null
                        enclosure = null
                        duration = null
                        image = null
                    }
                    "title" -> if (inItem) title = parser.nextText()
                    "author" -> if (inItem && artist.isNullOrBlank()) {
                        artist = parser.nextText()
                    }
                    "duration" -> if (inItem) duration = parser.nextText()
                    "enclosure" -> if (inItem) {
                        enclosure = parser.getAttributeValue(null, "url") ?: enclosure
                    }
                    "image" -> if (inItem) {
                        image = parser.getAttributeValue(null, "href")
                            ?: parser.getAttributeValue(null, "url")
                            ?: image
                    }
                    "content" -> if (inItem && enclosure.isNullOrBlank()) {
                        val type = parser.getAttributeValue(null, "type").orEmpty()
                        if (type.startsWith("audio") || type.isEmpty()) {
                            enclosure = parser.getAttributeValue(null, "url") ?: enclosure
                        }
                    }
                }
            }
            XmlPullParser.END_TAG -> if (parser.name.equals("item", ignoreCase = true)) {
                val url = enclosure
                if (!url.isNullOrBlank() && !title.isNullOrBlank()) {
                    episodes += ExtraTrack(
                        mediaId = ExtraMediaIds.podcast(url),
                        title = title!!.trim(),
                        artist = artist?.trim() ?: feedUrl,
                        durationText = duration?.trim(),
                        thumbnailUrl = image,
                        source = "Podcast"
                    )
                }
                inItem = false
            }
        }
        event = parser.next()
    }
    return episodes
}

internal fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        instanceFollowRedirects = true
        connectTimeout = 15_000
        readTimeout = 15_000
        setRequestProperty("User-Agent", "MiMusic/0.7.1")
        headers.forEach { (name, value) -> setRequestProperty(name, value) }
    }
    val stream = if (connection.responseCode >= 400) connection.errorStream else connection.inputStream
    val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    val code = connection.responseCode
    connection.disconnect()
    if (code !in 200..299) error("HTTP $code")
    return body
}
