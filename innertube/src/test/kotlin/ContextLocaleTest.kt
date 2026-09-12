package it.vfsfitvnm.innertube

import it.vfsfitvnm.innertube.models.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextLocaleTest {
    @Test
    fun defaultLocaleIsArabicEgypt() {
        assertEquals("ar", Context.hl)
        assertEquals("EG", Context.gl)
        assertEquals("ar", Context.DefaultWeb.client.hl)
        assertEquals("EG", Context.DefaultAndroidVR.client.gl)
    }

    @Test
    fun androidVrClientUsesDirectStreamHost() {
        val client = Context.DefaultAndroidVR.client
        assertEquals("ANDROID_VR", client.clientName)
        assertEquals("www.youtube.com", client.host)
        assertTrue(client.userAgent!!.contains("youtube.vr.oculus"))
    }
}
