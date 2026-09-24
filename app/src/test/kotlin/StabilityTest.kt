package it.vfsfitvnm.vimusic

import it.vfsfitvnm.vimusic.service.DownloadProgress
import it.vfsfitvnm.vimusic.service.LibraryQueue
import it.vfsfitvnm.vimusic.service.SleepTimerClock
import it.vfsfitvnm.vimusic.service.StreamUrlCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StabilityTest {
    @Test
    fun sleepTimerStopsAtZero() {
        assertEquals(4_000L, SleepTimerClock.remainingAfterTick(5_000L))
        assertNull(SleepTimerClock.remainingAfterTick(1_000L))
        assertNull(SleepTimerClock.remainingAfterTick(null))
    }

    @Test
    fun streamCacheDropsTheOldestUrl() {
        val cache = StreamUrlCache<String>(maxSize = 2)
        cache.put("a", "one")
        cache.put("b", "two")
        cache.put("c", "three")
        assertNull(cache.get("a"))
        assertEquals("three", cache.get("c"))
    }

    @Test
    fun downloadResumeKeepsAPartialFile() {
        assertEquals(0L, DownloadProgress.startPosition(0L, 1_000L))
        assertEquals(400L, DownloadProgress.startPosition(400L, 1_000L))
        assertEquals(40, DownloadProgress.percent(400L, 1_000L))
        assertEquals(0, DownloadProgress.percent(10L, null))
    }

    @Test
    fun libraryQueueStartsAtTheSelectedSong() {
        val ids = listOf("a", "b", "c")
        assertEquals(listOf("b", "c"), LibraryQueue.fromSelection(ids, "b"))
        assertEquals(ids, LibraryQueue.fromSelection(ids, "a"))
        assertEquals(true, LibraryQueue.isLibraryId("songs/abc"))
        assertEquals(false, LibraryQueue.isLibraryId("dQw4w9WgXcQ"))
    }
}
