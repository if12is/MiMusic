package it.vfsfitvnm.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.Context
import it.vfsfitvnm.innertube.models.PlayerResponse
import it.vfsfitvnm.innertube.models.bodies.PlayerBody
import it.vfsfitvnm.innertube.utils.runCatchingNonCancellable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private val playerClients: List<Context>
    get() = listOf(
        Context.DefaultAndroidVR,
        Context.DefaultAndroid,
        Context.DefaultIos,
        Context.DefaultAndroidMusic,
        Context.DefaultAgeRestrictionBypass
    )

private val streamProxyUrls = listOf(
    "https://pipedapi.kavin.rocks/streams/",
    "https://pipedapi.adminforge.de/streams/",
    "https://pipedapi.tokhmi.xyz/streams/",
    "https://api.piped.projectsegfau.lt/streams/",
    "https://inv.nadeko.net/api/v1/videos/",
    "https://yewtu.be/api/v1/videos/",
    "https://invidious.nerdvpn.de/api/v1/videos/",
    "https://iv.ggtyler.dev/api/v1/videos/"
)

private data class AudioStream(
    val url: String,
    val bitrate: Long? = null,
    val mimeType: String? = null,
    val itag: Int? = null
)

private fun PlayerResponse.hasPlayableAudio(): Boolean =
    playabilityStatus?.status == "OK" && streamingData?.highestQualityFormat?.url != null

private suspend fun Innertube.requestPlayer(body: PlayerBody, context: Context): PlayerResponse {
    val requestContext = when {
        context.client.clientName == "TVHTML5_SIMPLY_EMBEDDED_PLAYER" ->
            context.copy(
                thirdParty = Context.ThirdParty(
                    embedUrl = "https://www.youtube.com/watch?v=${body.videoId}"
                )
            )
        else -> context
    }

    return client.post(player) {
        url {
            host = requestContext.client.host
        }
        requestContext.client.userAgent?.let { userAgent ->
            header(HttpHeaders.UserAgent, userAgent)
        }
        header("X-YouTube-Client-Name", requestContext.client.clientName)
        header("X-YouTube-Client-Version", requestContext.client.clientVersion)
        setBody(
            body.copy(
                context = requestContext,
                contentCheckOk = true,
                racyCheckOk = true
            )
        )
    }.body()
}

private suspend fun Innertube.proxyAudioStreams(videoId: String): List<AudioStream> {
    for (baseUrl in streamProxyUrls) {
        val streams = runCatching {
            parseProxyStreams(
                client.get("$baseUrl$videoId") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.UserAgent, "Mozilla/5.0")
                }.body()
            )
        }.getOrNull()

        if (!streams.isNullOrEmpty()) {
            return streams
        }
    }

    return emptyList()
}

private fun parseProxyStreams(root: JsonObject): List<AudioStream> {
    val formats = root["audioStreams"]?.jsonArray
        ?: root["adaptiveFormats"]?.jsonArray
        ?: return emptyList()

    return formats.mapNotNull { element ->
        val item = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        val url = item["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        val mimeType = item["mimeType"]?.jsonPrimitive?.contentOrNull
            ?: item["type"]?.jsonPrimitive?.contentOrNull
        val bitrate = item["bitrate"]?.jsonPrimitive?.longOrNull
            ?: item["bitrate"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        val itag = item["itag"]?.jsonPrimitive?.intOrNull
            ?: item["itag"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

        val isAudio = mimeType?.contains("audio", ignoreCase = true) == true ||
            item["audioQuality"] != null ||
            root["audioStreams"] != null

        if (!isAudio) return@mapNotNull null

        AudioStream(
            url = url,
            bitrate = bitrate,
            mimeType = mimeType,
            itag = itag
        )
    }
}

private fun PlayerResponse.withAudioStreams(
    videoId: String,
    audioStreams: List<AudioStream>
): PlayerResponse {
    val existing = streamingData?.adaptiveFormats.orEmpty()
    val merged = if (existing.isNotEmpty()) {
        existing.map { adaptiveFormat ->
            adaptiveFormat.copy(
                url = audioStreams.find { stream ->
                    stream.bitrate == adaptiveFormat.bitrate || stream.itag == adaptiveFormat.itag
                }?.url ?: adaptiveFormat.url
            )
        }
    } else {
        audioStreams.mapIndexed { index, stream ->
            PlayerResponse.StreamingData.AdaptiveFormat(
                itag = stream.itag ?: (140 + index),
                mimeType = stream.mimeType ?: "audio/mp4",
                bitrate = stream.bitrate,
                url = stream.url
            )
        }
    }

    return copy(
        playabilityStatus = PlayerResponse.PlayabilityStatus(status = "OK"),
        streamingData = PlayerResponse.StreamingData(adaptiveFormats = merged),
        videoDetails = videoDetails ?: PlayerResponse.VideoDetails(videoId = videoId)
    )
}

suspend fun Innertube.player(body: PlayerBody) = runCatchingNonCancellable {
    var lastResponse: PlayerResponse? = null

    for (context in playerClients) {
        val response = runCatching {
            requestPlayer(body, context)
        }.getOrNull() ?: continue

        lastResponse = response

        if (response.hasPlayableAudio()) {
            return@runCatchingNonCancellable response
        }
    }

    val audioStreams = proxyAudioStreams(body.videoId)
    if (audioStreams.isNotEmpty()) {
        return@runCatchingNonCancellable (lastResponse ?: PlayerResponse(
            playabilityStatus = PlayerResponse.PlayabilityStatus(status = "OK"),
            playerConfig = null,
            streamingData = null,
            videoDetails = PlayerResponse.VideoDetails(videoId = body.videoId)
        )).withAudioStreams(body.videoId, audioStreams)
    }

    lastResponse ?: error("Unable to resolve a playable stream")
}
