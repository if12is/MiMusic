package it.vfsfitvnm.innertube.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtraMediaIdsTest {
    @Test
    fun encodesAndDecodesSoundCloudUrls() {
        val url = "https://soundcloud.com/artist/track-name"
        val mediaId = ExtraMediaIds.soundCloud(url)
        assertTrue(mediaId.startsWith(ExtraMediaIds.SOUNDCLOUD_PREFIX))
        assertEquals(url, ExtraMediaIds.payload(mediaId))
        assertTrue(ExtraMediaIds.isExternal(mediaId))
    }

    @Test
    fun encodesPodcastAndJellyfinPayloads() {
        val enclosure = "https://feeds.example.com/ep1.mp3"
        val podcastId = ExtraMediaIds.podcast(enclosure)
        assertEquals(enclosure, ExtraMediaIds.payload(podcastId))

        val itemId = "abc-123"
        assertEquals(itemId, ExtraMediaIds.payload(ExtraMediaIds.jellyfin(itemId)))
        assertFalse(ExtraMediaIds.isExternal("dQw4w9WgXcQ"))
        assertFalse(ExtraMediaIds.isExternal("local:12"))
    }
}
