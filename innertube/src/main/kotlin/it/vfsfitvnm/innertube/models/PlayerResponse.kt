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

        /**
         * A single file that already contains the picture and the sound (itag 22, then 18).
         * Video-only adaptive streams are not muxed; those are paired in [chooseVideo].
         */
        val muxedFallbackFormat: AdaptiveFormat?
            get() = chooseVideo()?.takeIf { it.audio == null }?.video

        /**
         * What to play when the user asked for the music video.
         * Prefers a progressive file. Otherwise pairs a video-only stream with audio
         * so the picture and the song both play.
         */
        fun chooseVideo(): VideoStreamChoice? {
            val pictures = (formats.orEmpty() + adaptiveFormats.orEmpty())
                .filter { !it.url.isNullOrBlank() && it.hasVideoPicture }
            val progressive = listOf(22, 18, 37, 38)
                .firstNotNullOfOrNull { tag -> pictures.find { it.itag == tag } }
                ?: pictures.find { it.isProgressiveMuxed }
            if (progressive != null) return VideoStreamChoice(video = progressive)

            val separate = listOf(136, 135, 134, 137, 133, 160, 247, 244, 243, 242, 248)
                .firstNotNullOfOrNull { tag ->
                    pictures.find { it.itag == tag && !it.isProgressiveMuxed }
                }
                ?: pictures.filterNot { it.isProgressiveMuxed }
                    .maxByOrNull { it.bitrate ?: 0L }
            val audio = playableAudioFormats.maxByOrNull { it.bitrate ?: it.averageBitrate ?: 0L }
            if (separate != null && !audio?.url.isNullOrBlank()) {
                return VideoStreamChoice(video = separate, audio = audio)
            }
            return null
        }

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

            /** A moving picture, not an audio-only track. */
            val hasVideoPicture: Boolean
                get() = !isAudioOnly && (
                    mimeType.contains("video", ignoreCase = true) ||
                        itag == 18 || itag == 22 || itag == 37 || itag == 38
                    )

            /** Progressive file with both picture and sound, not a video-only adaptive track. */
            val isProgressiveMuxed: Boolean
                get() {
                    if (isAudioOnly) return false
                    if (itag == 18 || itag == 22 || itag == 37 || itag == 38) return true
                    val mime = mimeType.lowercase()
                    val codecsHaveAudio = mime.contains("mp4a") ||
                        mime.contains("opus") ||
                        mime.contains("vorbis")
                    return mime.contains("video") && codecsHaveAudio
                }
        }
    }

    data class VideoStreamChoice(
        val video: StreamingData.AdaptiveFormat,
        val audio: StreamingData.AdaptiveFormat? = null
    )

    @Serializable
    data class VideoDetails(
        val videoId: String? = null,
        /** e.g. MUSIC_VIDEO_TYPE_ATV (song with static art) or MUSIC_VIDEO_TYPE_OMV (real video). */
        val musicVideoType: String? = null
    )
}
