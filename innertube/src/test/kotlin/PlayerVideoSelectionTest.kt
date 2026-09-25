package it.vfsfitvnm.innertube

import it.vfsfitvnm.innertube.models.PlayerResponse
import it.vfsfitvnm.innertube.requests.hasRealMusicVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun progressiveItag22BeatsASmallerVideoOnlyStream() {
        val response = PlayerResponse(
            streamingData = PlayerResponse.StreamingData(
                adaptiveFormats = listOf(
                    format(itag = 136, mime = "video/mp4", url = "https://example.com/video-only", bitrate = 90_000),
                    format(itag = 140, mime = "audio/mp4", url = "https://example.com/audio", bitrate = 128_000)
                ),
                formats = listOf(
                    format(itag = 22, mime = "video/mp4; codecs=\"avc1, mp4a.40.2\"", url = "https://example.com/muxed", bitrate = 700_000)
                )
            ),
            videoDetails = PlayerResponse.VideoDetails(videoId = "song", musicVideoType = "MUSIC_VIDEO_TYPE_OMV")
        )
        val choice = response.streamingData?.chooseVideo()
        assertEquals(22, choice?.video?.itag)
        assertNull(choice?.audio)
        assertEquals(22, response.streamingData?.muxedFallbackFormat?.itag)
        assertTrue(response.hasRealMusicVideo())
    }

    @Test
    fun videoOnlyTrackIsPairedWithAudio() {
        val response = PlayerResponse(
            streamingData = PlayerResponse.StreamingData(
                adaptiveFormats = listOf(
                    format(itag = 136, mime = "video/mp4", url = "https://example.com/picture", bitrate = 400_000),
                    format(itag = 160, mime = "video/mp4", url = "https://example.com/tiny", bitrate = 50_000),
                    format(itag = 140, mime = "audio/mp4", url = "https://example.com/sound", bitrate = 128_000)
                )
            ),
            videoDetails = PlayerResponse.VideoDetails(videoId = "song", musicVideoType = "MUSIC_VIDEO_TYPE_OMV")
        )
        val choice = response.streamingData?.chooseVideo()
        assertEquals(136, choice?.video?.itag)
        assertEquals("https://example.com/picture", choice?.video?.url)
        assertEquals(140, choice?.audio?.itag)
        assertEquals("https://example.com/sound", choice?.audio?.url)
        assertNull(response.streamingData?.muxedFallbackFormat)
        assertTrue(response.hasRealMusicVideo())
    }

    @Test
    fun muxedFileWithAudioQualityStillHasPictureAndSound() {
        val format = PlayerResponse.StreamingData.AdaptiveFormat(
            itag = 18,
            mimeType = "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"",
            audioQuality = "AUDIO_QUALITY_LOW",
            bitrate = 400_000,
            url = "https://example.com/both"
        )
        val response = PlayerResponse(
            streamingData = PlayerResponse.StreamingData(formats = listOf(format)),
            videoDetails = PlayerResponse.VideoDetails(
                videoId = "song",
                musicVideoType = "MUSIC_VIDEO_TYPE_OMV"
            )
        )
        assertFalse(format.isAudioOnly)
        val choice = response.streamingData?.chooseVideo()
        assertEquals(18, choice?.video?.itag)
        assertEquals("https://example.com/both", choice?.video?.url)
        assertNull(choice?.audio)
        assertTrue(response.hasRealMusicVideo())
    }

    @Test
    fun videoPictureWithoutSeparateAudioIsStillAVideo() {
        val response = PlayerResponse(
            streamingData = PlayerResponse.StreamingData(
                adaptiveFormats = listOf(
                    format(itag = 136, mime = "video/mp4", url = "https://example.com/picture", bitrate = 400_000)
                )
            ),
            videoDetails = PlayerResponse.VideoDetails(videoId = "song", musicVideoType = "MUSIC_VIDEO_TYPE_OMV")
        )
        val choice = response.streamingData?.chooseVideo()
        assertEquals(136, choice?.video?.itag)
        assertNull(choice?.audio)
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

    private fun format(itag: Int, mime: String, url: String, bitrate: Long) =
        PlayerResponse.StreamingData.AdaptiveFormat(
            itag = itag,
            mimeType = mime,
            bitrate = bitrate,
            url = url
        )

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
