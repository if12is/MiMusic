package it.vfsfitvnm.innertube

import it.vfsfitvnm.innertube.requests.DefaultLandingVideoId
import it.vfsfitvnm.innertube.requests.landingPage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandingPageRequestTest {
    @Test
    fun landingPageReturnsContentForArabicLocale() = LiveNetworkTest.runOrSkip {
        val page = Innertube.landingPage(DefaultLandingVideoId)?.getOrThrow()

        assertNotNull("Landing page should not be null", page)
        assertFalse("Landing page should not be empty", page!!.isEmpty)
        assertTrue(
            "Arabic landing should include songs, playlists, albums, or artists",
            !page.songs.isNullOrEmpty() ||
                !page.playlists.isNullOrEmpty() ||
                !page.albums.isNullOrEmpty() ||
                !page.artists.isNullOrEmpty()
        )
    }
}
