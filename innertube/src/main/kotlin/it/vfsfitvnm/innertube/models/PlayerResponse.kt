package it.vfsfitvnm.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus?,
    val playerConfig: PlayerConfig?,
    val streamingData: StreamingData?,
    val videoDetails: VideoDetails?,
) {
    @Serializable
    data class PlayabilityStatus(
        val status: String?,
        val reason: String? = null
    )

    @Serializable
    data class PlayerConfig(
        val audioConfig: AudioConfig?
    ) {
        @Serializable
        data class AudioConfig(
            private val loudnessDb: Double?
        ) {
            // For music clients only
            val normalizedLoudnessDb: Float?
                get() = loudnessDb?.plus(7)?.toFloat()
        }
    }

    @Serializable
    data class StreamingData(
        val adaptiveFormats: List<AdaptiveFormat>? = null,
        val formats: List<AdaptiveFormat>? = null
    ) {
        val highestQualityFormat: AdaptiveFormat?
            get() {
                val candidates = (adaptiveFormats.orEmpty() + formats.orEmpty())
                    .filter { !it.url.isNullOrBlank() }

                return candidates.findLast { it.itag == 251 || it.itag == 140 }
                    ?: candidates
                        .filter { format ->
                            format.mimeType.contains("audio", ignoreCase = true) ||
                                format.audioQuality != null
                        }
                        .maxByOrNull { it.bitrate ?: it.averageBitrate ?: 0L }
                    ?: candidates.maxByOrNull { it.bitrate ?: it.averageBitrate ?: 0L }
            }

        @Serializable
        data class AdaptiveFormat(
            val itag: Int,
            val mimeType: String,
            val bitrate: Long? = null,
            val averageBitrate: Long? = null,
            val contentLength: Long? = null,
            val audioQuality: String? = null,
            val approxDurationMs: Long? = null,
            val lastModified: Long? = null,
            val loudnessDb: Double? = null,
            val audioSampleRate: Int? = null,
            val url: String? = null,
            val signatureCipher: String? = null,
            val cipher: String? = null
        )
    }

    @Serializable
    data class VideoDetails(
        val videoId: String?
    )
}
