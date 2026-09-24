package it.vfsfitvnm.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.Context
import it.vfsfitvnm.innertube.models.PlayerResponse
import it.vfsfitvnm.innertube.models.bodies.PlayerBody
import it.vfsfitvnm.innertube.utils.NewPipeSupport
import it.vfsfitvnm.innertube.utils.PlayerLog
import it.vfsfitvnm.innertube.utils.ResolvedAudioStream
import it.vfsfitvnm.innertube.utils.newPipeAudioStreams
import it.vfsfitvnm.innertube.utils.runCatchingNonCancellable
import it.vfsfitvnm.innertube.utils.withDecipheredUrls
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager

private val playerClients: List<Context>
    get() = listOf(
        Context.DefaultAndroid,
        Context.DefaultAndroidMusic,
        Context.DefaultIos,
        Context.DefaultWebWatch,
        Context.DefaultWeb,
        Context.DefaultAndroidVR,
        Context.DefaultAgeRestrictionBypass
    )

private fun PlayerResponse.hasPlayableAudio(): Boolean =
    playabilityStatus?.status == "OK" && streamingData?.highestQualityFormat?.url != null

/** A progressive music video, not a song whose picture never moves. */
fun PlayerResponse.hasRealMusicVideo(): Boolean =
    streamingData?.muxedFallbackFormat?.url != null &&
        videoDetails?.musicVideoType != "MUSIC_VIDEO_TYPE_ATV"

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
        val signatureTimestamp = if (requestContext.client.clientName.startsWith("WEB")) {
            runCatching {
                NewPipeSupport.ensureInitialized()
                YoutubeJavaScriptPlayerManager.getSignatureTimestamp(body.videoId)
            }.onFailure { error ->
                PlayerLog.append("sts failed: ${error.message}")
            }.getOrNull()
        } else {
            null
        }

        setBody(
            body.copy(
                context = requestContext,
                contentCheckOk = true,
                racyCheckOk = true,
                playbackContext = signatureTimestamp?.let { timestamp ->
                    PlayerBody.PlaybackContext(
                        contentPlaybackContext = PlayerBody.ContentPlaybackContext(
                            signatureTimestamp = timestamp
                        )
                    )
                }
            )
        )
    }.body()
}

private fun PlayerResponse.withAudioStreams(
    videoId: String,
    audioStreams: List<ResolvedAudioStream>
): PlayerResponse {
    val existing = streamingData?.adaptiveFormats.orEmpty()
    val merged = audioStreams.mapIndexed { index, stream ->
        val match = existing.find { format ->
            (stream.itag != null && format.itag == stream.itag) ||
                (stream.bitrate != null && format.bitrate == stream.bitrate)
        }

        match?.copy(
            url = stream.url,
            mimeType = stream.mimeType ?: match.mimeType,
            bitrate = stream.bitrate ?: match.bitrate
        ) ?: PlayerResponse.StreamingData.AdaptiveFormat(
            itag = stream.itag ?: (140 + index),
            mimeType = stream.mimeType ?: "audio/mp4",
            bitrate = stream.bitrate,
            url = stream.url
        )
    }

    return copy(
        playabilityStatus = PlayerResponse.PlayabilityStatus(status = "OK"),
        streamingData = PlayerResponse.StreamingData(adaptiveFormats = merged),
        videoDetails = videoDetails ?: PlayerResponse.VideoDetails(videoId = videoId)
    )
}

suspend fun Innertube.player(body: PlayerBody, preferVideo: Boolean = false) = runCatchingNonCancellable {
    PlayerLog.append("resolve ${body.videoId} hl=${Context.hl} gl=${Context.gl} video=$preferVideo")
    var lastResponse: PlayerResponse? = null
    var audioResponse: PlayerResponse? = null

    for (context in playerClients) {
        val response = runCatching {
            requestPlayer(body, context)
        }.onFailure { error ->
            PlayerLog.append("${context.client.clientName} failed: ${error.message}")
        }.getOrNull() ?: continue

        val formatCount = response.streamingData?.adaptiveFormats.orEmpty().size +
            response.streamingData?.formats.orEmpty().size
        val urlCount = (response.streamingData?.adaptiveFormats.orEmpty() +
            response.streamingData?.formats.orEmpty()).count { !it.url.isNullOrBlank() }
        val cipherCount = (response.streamingData?.adaptiveFormats.orEmpty() +
            response.streamingData?.formats.orEmpty()).count {
            !it.signatureCipher.isNullOrBlank() || !it.cipher.isNullOrBlank()
        }
        PlayerLog.append(
            "${context.client.clientName} status=${response.playabilityStatus?.status} " +
                "reason=${response.playabilityStatus?.reason} formats=$formatCount urls=$urlCount ciphers=$cipherCount"
        )

        val unlocked = if (cipherCount > 0 || urlCount > 0) {
            response.withDecipheredUrls(body.videoId)
        } else {
            response
        }
        lastResponse = unlocked
        if (preferVideo && unlocked.hasRealMusicVideo()) {
            val muxed = unlocked.streamingData?.muxedFallbackFormat
            PlayerLog.append(
                "using video ${context.client.clientName} itag=${muxed?.itag} mime=${muxed?.mimeType}"
            )
            return@runCatchingNonCancellable unlocked
        }
        val unlockedAudio = unlocked.streamingData?.playableAudioFormats.orEmpty()
        if (unlocked.hasPlayableAudio()) {
            if (audioResponse == null) audioResponse = unlocked
            if (!preferVideo) {
                val format = unlocked.streamingData?.highestQualityFormat
                PlayerLog.append(
                    "using ${context.client.clientName} itag=${format?.itag} mime=${format?.mimeType} " +
                        "audioUrls=${unlockedAudio.size}"
                )
                return@runCatchingNonCancellable unlocked
            }
        }

        val muxed = unlocked.streamingData?.muxedFallbackFormat
        if (muxed != null && !preferVideo) {
            PlayerLog.append(
                "${context.client.clientName} keeping muxed fallback itag=${muxed.itag} mime=${muxed.mimeType}"
            )
        }
    }

    PlayerLog.append("no audio-only URL from InnerTube, trying NewPipe extractor")
    val audioStreams = runCatching {
        newPipeAudioStreams(body.videoId)
    }.onFailure { error ->
        PlayerLog.append("NewPipe failed: ${error.message}")
    }.getOrDefault(emptyList())
    val newPipeAudioOnly = audioStreams.filter { stream ->
        stream.mimeType?.contains("audio", ignoreCase = true) == true
    }
    if (preferVideo) {
        val muxedNewPipe = audioStreams.filter { stream ->
            val mime = stream.mimeType.orEmpty()
            mime.isNotBlank() && !mime.contains("audio", ignoreCase = true)
        }
        if (muxedNewPipe.isNotEmpty()) {
            PlayerLog.append("using NewPipe video streams=${muxedNewPipe.size}")
            return@runCatchingNonCancellable (lastResponse ?: PlayerResponse(
                playabilityStatus = PlayerResponse.PlayabilityStatus(status = "OK"),
                playerConfig = null,
                streamingData = null,
                videoDetails = PlayerResponse.VideoDetails(videoId = body.videoId)
            )).withAudioStreams(body.videoId, muxedNewPipe)
        }
        audioResponse?.let { saved ->
            PlayerLog.append("no music video, falling back to audio")
            return@runCatchingNonCancellable saved
        }
    }
    if (newPipeAudioOnly.isNotEmpty()) {
        PlayerLog.append("using NewPipe audio streams=${newPipeAudioOnly.size}")
        return@runCatchingNonCancellable (lastResponse ?: PlayerResponse(
            playabilityStatus = PlayerResponse.PlayabilityStatus(status = "OK"),
            playerConfig = null,
            streamingData = null,
            videoDetails = PlayerResponse.VideoDetails(videoId = body.videoId)
        )).withAudioStreams(body.videoId, newPipeAudioOnly)
    }

    val muxedResponse = lastResponse
    val muxed = muxedResponse?.streamingData?.muxedFallbackFormat
    if (muxedResponse != null && muxed?.url != null) {
        PlayerLog.append("using muxed fallback itag=${muxed.itag} mime=${muxed.mimeType}")
        return@runCatchingNonCancellable muxedResponse
    }

    if (audioStreams.isNotEmpty()) {
        PlayerLog.append("using NewPipe muxed streams=${audioStreams.size}")
        return@runCatchingNonCancellable (lastResponse ?: PlayerResponse(
            playabilityStatus = PlayerResponse.PlayabilityStatus(status = "OK"),
            playerConfig = null,
            streamingData = null,
            videoDetails = PlayerResponse.VideoDetails(videoId = body.videoId)
        )).withAudioStreams(body.videoId, audioStreams)
    }

    val status = lastResponse?.playabilityStatus
    PlayerLog.append("unresolved status=${status?.status} reason=${status?.reason}")
    lastResponse ?: error("Unable to resolve a playable stream")
}
