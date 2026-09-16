package it.vfsfitvnm.vimusic.utils

import it.vfsfitvnm.innertube.models.PlayerResponse
import it.vfsfitvnm.vimusic.enums.AudioQuality

fun PlayerResponse.StreamingData.formatFor(
    quality: AudioQuality,
    preferMuxed: Boolean = false
): PlayerResponse.StreamingData.AdaptiveFormat? {
    if (preferMuxed) {
        return muxedFallbackFormat ?: formatFor(quality, preferMuxed = false)
    }
    val audio = playableAudioFormats
    return when (quality) {
        AudioQuality.Auto -> playableFormat
        AudioQuality.High -> highestQualityFormat ?: muxedFallbackFormat
        AudioQuality.Medium -> audio.find { it.itag == 140 }
            ?: audio.sortedBy { it.bitrate ?: 0L }.let { list ->
                list.getOrNull(list.size / 2)
            }
            ?: playableFormat
        AudioQuality.Low -> audio.minByOrNull { it.bitrate ?: Long.MAX_VALUE }
            ?: muxedFallbackFormat
    }
}
