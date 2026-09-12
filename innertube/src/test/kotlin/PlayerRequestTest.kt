package it.vfsfitvnm.innertube

import it.vfsfitvnm.innertube.models.bodies.PlayerBody
import it.vfsfitvnm.innertube.requests.player
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class PlayerRequestTest {
    @Test
    fun playerReturnsDownloadableAudioUrl() = runBlocking {
        val response = Innertube.player(PlayerBody(videoId = "dQw4w9WgXcQ"))
            ?.getOrThrow()

        assertNotNull("Player response should not be null", response)
        assertTrue(
            "Playability status should be OK",
            response!!.playabilityStatus?.status == "OK"
        )

        val format = response.streamingData?.highestQualityFormat
        assertNotNull("A playable audio format should be available", format)
        assertNotNull("The audio format should include a stream URL", format!!.url)

        val connection = (URL(format.url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Range", "bytes=0-2047")
            connectTimeout = 15000
            readTimeout = 15000
        }

        val status = connection.responseCode
        val bytes = connection.inputStream.use { it.readBytes() }
        connection.disconnect()

        assertTrue("Stream should be reachable, got HTTP $status", status in 200..206)
        assertTrue("Stream should return audio bytes", bytes.isNotEmpty())
    }
}
