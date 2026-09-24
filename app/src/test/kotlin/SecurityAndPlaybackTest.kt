package it.vfsfitvnm.vimusic

import it.vfsfitvnm.vimusic.enums.AppLanguage
import it.vfsfitvnm.vimusic.ui.screens.home.regionalSections
import it.vfsfitvnm.vimusic.ui.styling.UiStrings
import it.vfsfitvnm.vimusic.utils.CleartextPolicy
import it.vfsfitvnm.vimusic.utils.PlaybackChoice
import it.vfsfitvnm.vimusic.utils.PlaybackPolicy
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAndPlaybackTest {
    @Test
    fun publicHttpIsBlockedAndLocalHttpStaysAvailable() {
        assertFalse(CleartextPolicy.allows("http://example.com/song"))
        assertFalse(CleartextPolicy.allows("http://8.8.8.8"))
        assertTrue(CleartextPolicy.allows("https://example.com/song"))
        assertTrue(CleartextPolicy.allows("http://192.168.1.20:8096"))
        assertTrue(CleartextPolicy.allows("http://10.0.0.5"))
        assertTrue(CleartextPolicy.allows("http://172.16.0.4"))
        assertTrue(CleartextPolicy.allows("http://localhost:8096"))
        assertTrue(CleartextPolicy.allows("http://jellyfin.local"))
    }

    @Test
    fun offlinePlaybackUsesLocalAudioBeforeTheNetwork() {
        assertEquals(PlaybackChoice.Downloaded, PlaybackPolicy.choose(true, true, false))
        assertEquals(PlaybackChoice.Downloaded, PlaybackPolicy.choose(true, false, false))
        assertEquals(PlaybackChoice.LocalCache, PlaybackPolicy.choose(false, true, true))
        assertEquals(PlaybackChoice.Unavailable, PlaybackPolicy.choose(false, true, false))
        assertEquals(PlaybackChoice.NeedNetwork, PlaybackPolicy.choose(false, false, false))
    }

    @Test
    fun regionalSectionsFollowTheSelectedCountry() {
        val year = Calendar.getInstance().get(Calendar.YEAR).toString()
        val egypt = regionalSections("EG", UiStrings(AppLanguage.Arabic), Locale("ar"))
        assertTrue(egypt.any { it.id == "eg_trending" && it.query.contains("مصر") && it.query.contains(year) })
        assertTrue(egypt.any { it.id == "arabic_classic" })

        val unitedStates = regionalSections("US", UiStrings(AppLanguage.English), Locale.ENGLISH)
        assertTrue(unitedStates.none { it.id.startsWith("arabic_") })
        assertTrue(unitedStates.any { it.query.contains("United States") && it.query.contains(year) })
        assertTrue(regionalSections(null, UiStrings(AppLanguage.English), Locale.ENGLISH).isEmpty())
    }
}
