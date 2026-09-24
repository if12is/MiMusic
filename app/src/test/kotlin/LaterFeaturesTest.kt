package it.vfsfitvnm.vimusic

import it.vfsfitvnm.vimusic.utils.newestBackupName
import it.vfsfitvnm.vimusic.utils.planDailyMixes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaterFeaturesTest {
    @Test
    fun dailyMixesStayOnDeviceAndChangeWithTheDay() {
        val songs = (1..12).map { index -> "song-$index" to "artist-${index % 4}" }
        val today = planDailyMixes(songs, 20_000)
        val tomorrow = planDailyMixes(songs, 20_001)
        assertEquals(3, today.size)
        assertTrue(today.all { it.songIds.isNotEmpty() })
        assertTrue(today.flatMap { it.songIds }.distinct().size == today.sumOf { it.songIds.size })
        assertTrue(today.map { it.songIds } != tomorrow.map { it.songIds })
    }

    @Test
    fun newestBackupIsTheLatestFileName() {
        assertEquals(
            "mimusic-20260924120000.db",
            newestBackupName(
                listOf("notes.txt", "mimusic-20260901120000.db", "mimusic-20260924120000.db")
            )
        )
        assertEquals(null, newestBackupName(listOf("playlist.m3u")))
    }
}
