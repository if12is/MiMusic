package it.vfsfitvnm.innertube.utils

import it.vfsfitvnm.innertube.models.PlayerResponse
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.net.URLDecoder

internal data class ResolvedAudioStream(
    val url: String,
    val bitrate: Long? = null,
    val mimeType: String? = null,
    val itag: Int? = null
)

internal fun parseQueryString(raw: String): Map<String, String> {
    return raw.split("&").mapNotNull { part ->
        val separator = part.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        val key = URLDecoder.decode(part.substring(0, separator), Charsets.UTF_8)
        val value = URLDecoder.decode(part.substring(separator + 1), Charsets.UTF_8)
        key to value
    }.toMap()
}

internal fun PlayerResponse.withDecipheredUrls(videoId: String): PlayerResponse {
    val data = streamingData ?: return this
    NewPipeSupport.ensureInitialized()
    return copy(
        streamingData = data.copy(
            adaptiveFormats = data.adaptiveFormats?.map { format ->
                format.withDecipheredUrl(videoId)
            },
            formats = data.formats?.map { format ->
                format.withDecipheredUrl(videoId)
            }
        )
    )
}

internal fun newPipeAudioStreams(videoId: String): List<ResolvedAudioStream> {
    NewPipeSupport.ensureInitialized()
    val info = StreamInfo.getInfo("https://www.youtube.com/watch?v=$videoId")
    val audio = info.audioStreams.mapNotNull { stream ->
        stream.toResolvedAudio()
    }
    if (audio.isNotEmpty()) return audio

    // Quran recitations and many official videos only expose muxed progressive MP4.
    return info.videoStreams.mapNotNull { stream ->
        stream.toResolvedAudio()
    }
}

private fun org.schabi.newpipe.extractor.stream.AudioStream.toResolvedAudio(): ResolvedAudioStream? {
    val url = content.takeIf { it.isNotBlank() } ?: return null
    val bitrate = averageBitrate.takeIf { it > 0 }?.let { kbps ->
        if (kbps < 10_000) kbps * 1000L else kbps.toLong()
    }
    return ResolvedAudioStream(
        url = url,
        bitrate = bitrate,
        mimeType = format?.mimeType,
        itag = itag
    )
}

private fun org.schabi.newpipe.extractor.stream.VideoStream.toResolvedAudio(): ResolvedAudioStream? {
    val url = content.takeIf { it.isNotBlank() } ?: return null
    return ResolvedAudioStream(
        url = url,
        bitrate = null,
        mimeType = format?.mimeType ?: "video/mp4",
        itag = itag
    )
}

private fun PlayerResponse.StreamingData.AdaptiveFormat.withDecipheredUrl(
    videoId: String
): PlayerResponse.StreamingData.AdaptiveFormat {
    return runCatching {
        val raw = url?.takeIf { it.isNotBlank() } ?: urlFromCipher(videoId) ?: return this
        val unlocked = YoutubeJavaScriptPlayerManager
            .getUrlWithThrottlingParameterDeobfuscated(videoId, raw)
        copy(url = unlocked)
    }.onFailure { error ->
        PlayerLog.append("decipher itag=$itag failed: ${error.message}")
    }.getOrDefault(this)
}

private fun PlayerResponse.StreamingData.AdaptiveFormat.urlFromCipher(videoId: String): String? {
    val cipher = signatureCipher ?: this.cipher ?: return null
    val params = parseQueryString(cipher)
    val baseUrl = params["url"] ?: return null
    val signature = params["s"] ?: return baseUrl
    val parameter = params["sp"] ?: "sig"
    val decoded = YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, signature)
    val joiner = if (baseUrl.contains('?')) "&" else "?"
    return "$baseUrl$joiner$parameter=$decoded"
}
