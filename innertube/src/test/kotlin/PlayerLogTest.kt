package it.vfsfitvnm.innertube

import it.vfsfitvnm.innertube.utils.PlayerLog
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLogTest {
    @Test
    fun keepsRecentResolveLines() {
        PlayerLog.clear()
        PlayerLog.append("resolve dQw4w9WgXcQ")
        PlayerLog.append("ANDROID_VR status=OK")
        val snapshot = PlayerLog.snapshot()
        assertTrue(snapshot.contains("resolve dQw4w9WgXcQ"))
        assertTrue(snapshot.contains("ANDROID_VR status=OK"))
        assertTrue(PlayerLog.lastSummary.contains("ANDROID_VR"))
    }
}
