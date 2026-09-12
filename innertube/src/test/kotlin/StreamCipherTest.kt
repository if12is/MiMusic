package it.vfsfitvnm.innertube

import it.vfsfitvnm.innertube.utils.parseQueryString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLEncoder

class StreamCipherTest {
    @Test
    fun parsesSignatureCipherFields() {
        val url = "https://rr1---sn-example.googlevideo.com/videoplayback?id=abc&n=obfuscated"
        val cipher = listOf(
            "s" to "AAA.BBB",
            "sp" to "sig",
            "url" to url
        ).joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, Charsets.UTF_8)}"
        }

        val parsed = parseQueryString(cipher)
        assertEquals("AAA.BBB", parsed["s"])
        assertEquals("sig", parsed["sp"])
        assertEquals(url, parsed["url"])
    }
}
