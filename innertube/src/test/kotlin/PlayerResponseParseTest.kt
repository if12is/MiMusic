package it.vfsfitvnm.innertube

import it.vfsfitvnm.innertube.models.PlayerResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PlayerResponseParseTest {
    private val json = Json {
        ignoreUnknownKeys = true
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
}
