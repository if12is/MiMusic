package it.vfsfitvnm.innertube

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LiveNetworkAssumptionTest {
    @Test
    fun treatsWrappedIoAsTransient() {
        val error = RuntimeException("request failed", IOException("timeout"))
        assertTrue(LiveNetworkTest.isTransient(error))
    }

    @Test
    fun doesNotTreatAssertionFailuresAsTransient() {
        assertFalse(LiveNetworkTest.isTransient(AssertionError("playability should be OK")))
    }
}
