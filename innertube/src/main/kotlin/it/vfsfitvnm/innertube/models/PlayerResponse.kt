package it.vfsfitvnm.innertube.models

import it.vfsfitvnm.innertube.utils.FlexibleIntSerializer
import it.vfsfitvnm.innertube.utils.FlexibleLongSerializer
import kotlinx.serialization.Serializable

@Serializable
data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus? = null,
    val playerConfig: PlayerConfig? = null,
    val streamingData: StreamingData? = null,
    val videoDetails: VideoDetails? = null,
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
                val audio = playableAudioFormats
                return audio.find { it.itag == 251 }
                    ?: audio.find { it.itag == 140 }
                    ?: audio.maxByOrNull { it.bitrate ?: it.averageBitrate ?: 0L }
            }

        val muxedFallbackFormat: AdaptiveFormat?
            get() = (adaptiveFormats.orEmpty() + formats.orEmpty())
                .filter { !it.url.isNullOrBlank() && !it.isAudioOnly }
                .filter { format ->
                    format.itag == 18 ||
                        format.itag == 22 ||
                        format.mimeType.contains("mp4", ignoreCase = true)
                }
                .minByOrNull { it.bitrate ?: Long.MAX_VALUE }

        val playableAudioFormats: List<AdaptiveFormat>
            get() = (adaptiveFormats.orEmpty() + formats.orEmpty())
                .filter { !it.url.isNullOrBlank() && it.isAudioOnly }

        val playableFormat: AdaptiveFormat?
            get() = highestQualityFormat ?: muxedFallbackFormat

        @Serializable
        data class AdaptiveFormat(
            val itag: Int,
            val mimeType: String,
            val bitrate: Long? = null,
            val averageBitrate: Long? = null,
            @Serializable(with = FlexibleLongSerializer::class)
            val contentLength: Long? = null,
            val audioQuality: String? = null,
            @Serializable(with = FlexibleLongSerializer::class)
            val approxDurationMs: Long? = null,
            @Serializable(with = FlexibleLongSerializer::class)
            val lastModified: Long? = null,
            val loudnessDb: Double? = null,
            @Serializable(with = FlexibleIntSerializer::class)
            val audioSampleRate: Int? = null,
            val url: String? = null,
            val signatureCipher: String? = null,
            val cipher: String? = null
        ) {
            val isAudioOnly: Boolean
                get() = mimeType.contains("audio", ignoreCase = true) || audioQuality != null
        }
    }

    @Serializable
    data class VideoDetails(
        val videoId: String? = null
    )
}
