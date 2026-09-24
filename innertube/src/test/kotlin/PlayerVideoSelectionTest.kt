package it.vfsfitvnm.innertube

import it.vfsfitvnm.innertube.models.PlayerResponse
import it.vfsfitvnm.innertube.requests.hasRealMusicVideo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVideoSelectionTest {
    @Test
    fun staticArtworkIsNotAMusicVideo() {
        val response = player(
            mime = "video/mp4",
            url = "https://example.com/atv",
            musicVideoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        assertFalse(response.hasRealMusicVideo())
    }

    @Test
    fun officialMusicVideoCanBeSelected() {
        val response = player(
            mime = "video/mp4",
            url = "https://example.com/omv",
            musicVideoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        assertTrue(response.hasRealMusicVideo())
    }

    @Test
    fun audioOnlyResponseIsNotAMusicVideo() {
        val response = player(
            mime = "audio/mp4",
            url = "https://example.com/audio",
            musicVideoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        assertFalse(response.hasRealMusicVideo())
    }

    private fun player(mime: String, url: String, musicVideoType: String) = PlayerResponse(
        streamingData = PlayerResponse.StreamingData(
            formats = listOf(
                PlayerResponse.StreamingData.AdaptiveFormat(
                    itag = 18,
                    mimeType = mime,
                    url = url
                )
            )
        ),
        videoDetails = PlayerResponse.VideoDetails(
            videoId = "song",
            musicVideoType = musicVideoType
        )
    )
}
