import it.vfsfitvnm.innertube.utils.compareVersionNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionNamesTest {
    @Test
    fun detectsNewerRelease() {
        assertTrue(compareVersionNames("0.6.1", "0.6.0") > 0)
        assertTrue(compareVersionNames("v0.7.0", "0.6.0") > 0)
        assertTrue(compareVersionNames("1.0.0", "0.9.9") > 0)
    }

    @Test
    fun treatsEqualVersionsAsCurrent() {
        assertEquals(0, compareVersionNames("0.6.0", "0.6.0"))
        assertEquals(0, compareVersionNames("v0.6.0", "0.6.0"))
    }

    @Test
    fun detectsOlderRelease() {
        assertTrue(compareVersionNames("0.5.4", "0.6.0") < 0)
    }
}
