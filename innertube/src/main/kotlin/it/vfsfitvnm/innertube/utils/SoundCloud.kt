package it.vfsfitvnm.innertube.utils

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Locale
import kotlin.math.max

data class ExtraTrack(
    val mediaId: String,
    val title: String,
    val artist: String?,
    val durationText: String?,
    val thumbnailUrl: String?,
    val source: String
)

fun searchSoundCloud(query: String, limit: Int = 30): List<ExtraTrack> {
    if (query.isBlank()) return emptyList()
    NewPipeSupport.ensureInitialized()
    val extractor = ServiceList.SoundCloud.getSearchExtractor(query.trim())
    extractor.fetchPage()
    return extractor.initialPage.items.mapNotNull { item ->
        val stream = item as? StreamInfoItem ?: return@mapNotNull null
        ExtraTrack(
            mediaId = ExtraMediaIds.soundCloud(stream.url),
            title = stream.name,
            artist = stream.uploaderName,
            durationText = formatSeconds(stream.duration),
            thumbnailUrl = stream.thumbnails.maxByOrNull { max(it.height, it.width) }?.url,
            source = "SoundCloud"
        )
    }.take(limit)
}

fun soundCloudStreamUrl(pageUrl: String): String {
    NewPipeSupport.ensureInitialized()
    val extractor = ServiceList.SoundCloud.getStreamExtractor(pageUrl)
    extractor.fetchPage()
    val audio = extractor.audioStreams.maxByOrNull { it.averageBitrate }
    val video = extractor.videoStreams.firstOrNull()
    val content = audio?.content ?: video?.content
    require(!content.isNullOrBlank()) { "No SoundCloud stream" }
    return content
}

private fun formatSeconds(seconds: Long): String? {
    if (seconds <= 0L) return null
    val total = seconds.toInt()
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, secs)
    }
}
