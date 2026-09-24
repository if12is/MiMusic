package it.vfsfitvnm.vimusic

import it.vfsfitvnm.innertube.utils.ExtraTrack
import it.vfsfitvnm.vimusic.enums.AppLanguage
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.models.SongWithContentLength
import it.vfsfitvnm.vimusic.utils.BassLevel
import it.vfsfitvnm.vimusic.utils.DownloadSort
import it.vfsfitvnm.vimusic.utils.EqualizerPreset
import it.vfsfitvnm.vimusic.utils.artworkRequestSize
import it.vfsfitvnm.vimusic.utils.bandGains
import it.vfsfitvnm.vimusic.utils.chartsOrSearch
import it.vfsfitvnm.vimusic.utils.displayed
import it.vfsfitvnm.vimusic.utils.formatByteSize
import it.vfsfitvnm.vimusic.utils.playbackLookahead
import it.vfsfitvnm.vimusic.utils.podcastTracks
import it.vfsfitvnm.vimusic.utils.quranSearchQuery
import it.vfsfitvnm.vimusic.utils.sortedDownloads
import it.vfsfitvnm.vimusic.utils.totalDownloadBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperienceTest {
    @Test
    fun equalizerBassBoostsTheLowBandsWithoutLeavingTheSafeRange() {
        val gains = EqualizerPreset.Bass.bandGains(5, -1500, 1500)
        assertTrue(gains[0] > gains[4])
        assertTrue(gains.all { it in -1500..1500 })
        assertEquals(0, EqualizerPreset.Flat.bandGains(5, -1500, 1500).sum())
        assertEquals(0, BassLevel.Off.strength.toInt())
        assertTrue(BassLevel.High.strength > BassLevel.Low.strength)
    }

    @Test
    fun hiddenLanguagesFallBackToEnglish() {
        assertEquals(AppLanguage.English, AppLanguage.French.displayed())
        assertEquals(AppLanguage.English, AppLanguage.Turkish.displayed())
        assertEquals(AppLanguage.English, AppLanguage.Urdu.displayed())
        assertEquals(AppLanguage.Arabic, AppLanguage.Arabic.displayed())
        assertEquals(listOf(AppLanguage.Arabic, AppLanguage.English), AppLanguage.translated)
    }

    @Test
    fun searchPlansKeepQuranAndPodcastsSeparate() {
        assertEquals("قرآن", quranSearchQuery(""))
        assertEquals("فيروز قرآن", quranSearchQuery("فيروز"))
        assertEquals("quran recitation", quranSearchQuery("quran recitation"))
        val tracks = listOf(
            track("a", "Podcast"),
            track("b", "SoundCloud"),
            track("c", "podcast")
        )
        assertEquals(listOf("a", "c"), podcastTracks(tracks).map { it.mediaId })
    }

    @Test
    fun chartsWinAndSearchIsTheFallback() {
        assertEquals(listOf("chart"), chartsOrSearch(listOf("chart"), listOf("search")))
        assertEquals(listOf("search"), chartsOrSearch(emptyList(), listOf("search")))
        assertEquals(emptyList<String>(), chartsOrSearch<String>(null, null))
    }

    @Test
    fun downloadsSortBySizeAndReportTheTotal() {
        val songs = listOf(download("b", "Bee", 100), download("a", "Aye", 400))
        assertEquals(listOf("a", "b"), songs.sortedDownloads(DownloadSort.Title).map { it.song.id })
        assertEquals(listOf("a", "b"), songs.sortedDownloads(DownloadSort.Size).map { it.song.id })
        assertEquals(500L, totalDownloadBytes(songs))
        assertEquals("1.0 KB", formatByteSize(1024))
    }

    @Test
    fun lowPowerShrinksArtworkAndPrefetch() {
        assertEquals(1, playbackLookahead(true))
        assertEquals(3, playbackLookahead(false))
        assertEquals(64, artworkRequestSize(128, true))
        assertEquals(128, artworkRequestSize(128, false))
    }

    private fun track(id: String, source: String) = ExtraTrack(id, id, null, null, null, source)

    private fun download(id: String, title: String, bytes: Long) = SongWithContentLength(
        song = Song(
            id = id,
            title = title,
            artistsText = null,
            durationText = "1:00",
            thumbnailUrl = null
        ),
        contentLength = bytes
    )
}
