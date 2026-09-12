package it.vfsfitvnm.innertube

import it.vfsfitvnm.innertube.models.PlayerResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PlayerResponseParseTest {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun parsesStringEncodedNumericFields() {
        val response = json.decodeFromString<PlayerResponse>(
            """
            {
              "playabilityStatus": { "status": "OK" },
              "videoDetails": { "videoId": "dQw4w9WgXcQ" },
              "streamingData": {
                "adaptiveFormats": [
                  {
                    "itag": 140,
                    "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
                    "bitrate": 131000,
                    "contentLength": "4123456",
                    "approxDurationMs": "213000",
                    "lastModified": "1766963492248817",
                    "audioSampleRate": "44100",
                    "audioQuality": "AUDIO_QUALITY_MEDIUM",
                    "url": "https://example.com/audio.mp4"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val format = response.streamingData?.highestQualityFormat
        assertNotNull(format)
        assertEquals(140, format!!.itag)
        assertEquals(4_123_456L, format.contentLength)
        assertEquals(213_000L, format.approxDurationMs)
        assertEquals(1_766_963_492_248_817L, format.lastModified)
        assertEquals(44_100, format.audioSampleRate)
        assertEquals("https://example.com/audio.mp4", format.url)
    }

    @Test
    fun stillParsesNumericFieldsAsNumbers() {
        val response = json.decodeFromString<PlayerResponse>(
            """
            {
              "playabilityStatus": { "status": "OK" },
              "streamingData": {
                "adaptiveFormats": [
                  {
                    "itag": 251,
                    "mimeType": "audio/webm",
                    "bitrate": 160000,
                    "contentLength": 2048,
                    "approxDurationMs": 1000,
                    "lastModified": 123,
                    "audioSampleRate": 48000,
                    "url": "https://example.com/a.webm"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val format = response.streamingData?.highestQualityFormat
        assertEquals(251, format?.itag)
        assertEquals(2048L, format?.contentLength)
        assertEquals(48_000, format?.audioSampleRate)
    }

    @Test
    fun ignoresMuxedVideoWhenSelectingAudio() {
        val response = json.decodeFromString<PlayerResponse>(
            """
            {
              "playabilityStatus": { "status": "OK" },
              "streamingData": {
                "adaptiveFormats": [
                  {
                    "itag": 140,
                    "mimeType": "audio/mp4",
                    "bitrate": 131000,
                    "audioQuality": "AUDIO_QUALITY_MEDIUM"
                  }
                ],
                "formats": [
                  {
                    "itag": 18,
                    "mimeType": "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"",
                    "bitrate": 500000,
                    "url": "https://example.com/itag18.mp4"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        assertEquals(null, response.streamingData?.highestQualityFormat)
        assertEquals(18, response.streamingData?.muxedFallbackFormat?.itag)
        assertEquals(18, response.streamingData?.playableFormat?.itag)
    }

    @Test
    fun prefersAudioUrlOverMuxedVideo() {
        val response = json.decodeFromString<PlayerResponse>(
            """
            {
              "playabilityStatus": { "status": "OK" },
              "streamingData": {
                "adaptiveFormats": [
                  {
                    "itag": 140,
                    "mimeType": "audio/mp4",
                    "bitrate": 131000,
                    "audioQuality": "AUDIO_QUALITY_MEDIUM",
                    "url": "https://example.com/audio.m4a"
                  }
                ],
                "formats": [
                  {
                    "itag": 18,
                    "mimeType": "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"",
                    "bitrate": 500000,
                    "url": "https://example.com/itag18.mp4"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        assertEquals(140, response.streamingData?.highestQualityFormat?.itag)
        assertEquals("https://example.com/audio.m4a", response.streamingData?.playableFormat?.url)
    }

    @Test
    fun playsQuranStyleMuxedProgressiveMp4() {
        val response = json.decodeFromString<PlayerResponse>(
            """
            {
              "playabilityStatus": { "status": "OK" },
              "streamingData": {
                "formats": [
                  {
                    "itag": 18,
                    "mimeType": "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"",
                    "bitrate": 395000,
                    "url": "https://example.com/quran-itag18.mp4"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val format = response.streamingData?.playableFormat
        assertEquals(18, format?.itag)
        assertEquals(false, format?.isAudioOnly)
        assertEquals("https://example.com/quran-itag18.mp4", format?.url)
    }
}
