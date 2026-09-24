package it.vfsfitvnm.innertube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeCookieAuthTest {
    @Test
    fun sapisidHashIsStableForAKnownCookie() {
        val header = Innertube.authorizationHeader("PREF=1; SAPISID=abc", nowSeconds = 1_700_000_000)
        assertTrue(header!!.startsWith("SAPISIDHASH 1700000000_"))
        assertEquals(header, Innertube.authorizationHeader("SAPISID=abc", nowSeconds = 1_700_000_000))
    }

    @Test
    fun cookieWithoutSapisidDoesNotInventAnAuthorizationHeader() {
        assertNull(Innertube.authorizationHeader("VISITOR_INFO1_LIVE=x"))
        assertNull(Innertube.authorizationHeader("  "))
        assertNull(Innertube.authorizationHeader(null))
    }
}
