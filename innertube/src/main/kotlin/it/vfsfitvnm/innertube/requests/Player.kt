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
import kotlinx.serialization.Serializable

private val playerClients: List<Context>
    get() = listOf(
        Context.DefaultAndroidVR,
        Context.DefaultAndroid,
        Context.DefaultIos,
        Context.DefaultAndroidMusic,
        Context.DefaultAgeRestrictionBypass
    )

private val pipedInstances = listOf(
    "https://pipedapi.kavin.rocks",
    "https://pipedapi.adminforge.de",
    "https://pipedapi.tokhmi.xyz",
    "https://api.piped.projectsegfau.lt"
)

@Serializable
private data class AudioStream(
    val url: String,
    val bitrate: Long? = null
)

@Serializable
private data class PipedResponse(
    val audioStreams: List<AudioStream> = emptyList()
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

private suspend fun Innertube.pipedAudioStreams(videoId: String): List<AudioStream> {
    for (instance in pipedInstances) {
        val streams = runCatching {
            client.get("$instance/streams/$videoId") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "Mozilla/5.0")
            }.body<PipedResponse>().audioStreams
        }.getOrNull()

        if (!streams.isNullOrEmpty()) {
            return streams
        }
    }

    return emptyList()
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

    val fallback = lastResponse
    if (fallback?.playabilityStatus?.status == "OK") {
        val audioStreams = pipedAudioStreams(body.videoId)
        if (audioStreams.isNotEmpty()) {
            return@runCatchingNonCancellable fallback.copy(
                streamingData = fallback.streamingData?.copy(
                    adaptiveFormats = fallback.streamingData.adaptiveFormats?.map { adaptiveFormat ->
                        adaptiveFormat.copy(
                            url = audioStreams.find { stream ->
                                stream.bitrate == adaptiveFormat.bitrate
                            }?.url ?: adaptiveFormat.url
                        )
                    } ?: audioStreams.mapIndexed { index, stream ->
                        PlayerResponse.StreamingData.AdaptiveFormat(
                            itag = 140 + index,
                            mimeType = "audio/mp4",
                            bitrate = stream.bitrate,
                            url = stream.url
                        )
                    }
                )
            )
        }
    }

    fallback ?: error("Unable to resolve a playable stream")
}
