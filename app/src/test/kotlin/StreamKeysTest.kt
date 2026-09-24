package it.vfsfitvnm.vimusic

import it.vfsfitvnm.vimusic.service.CastRanges
import it.vfsfitvnm.vimusic.service.StreamKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamKeysTest {
    @Test
    fun audioCacheKeyDoesNotSatisfyAVideoRequest() {
        val song = "dQw4w9WgXcQ"
        val audio = StreamKeys.of(song, wantsVideo = false)
        val video = StreamKeys.of(song, wantsVideo = true)

        assertEquals(song, audio)
        assertEquals("$song#video", video)
        assertEquals(audio, StreamKeys.songId(video))
        assertFalse(StreamKeys.sameStream(video, audio))
        assertFalse(StreamKeys.sameStream(StreamKeys.songId(video), video))
        assertTrue(StreamKeys.sameStream(video, video))
    }

    @Test
    fun castRangeKeepsASingleOpenInterval() {
        assertEquals(0L..9L, CastRanges.bounds(null, 10))
        assertEquals(2L..5L, CastRanges.bounds("bytes=2-5", 10))
        assertEquals(4L..9L, CastRanges.bounds("bytes=4-", 10))
        assertEquals(0L..0L, CastRanges.bounds("bytes=0-0", 0))
    }
}
