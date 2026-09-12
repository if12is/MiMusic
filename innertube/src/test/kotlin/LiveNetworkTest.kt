package it.vfsfitvnm.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Assume
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.TimeoutException

/**
 * Live YouTube / Piped checks are useful smoke tests, but GitHub runners
 * regularly hit timeouts and DNS failures. Retry a few times, then skip
 * instead of failing CI.
 */
internal object LiveNetworkTest {
    fun runOrSkip(attempts: Int = 3, block: suspend () -> Unit) = runBlocking {
        var lastFailure: Throwable? = null

        repeat(attempts) { attempt ->
            try {
                block()
                return@runBlocking
            } catch (error: Throwable) {
                if (!isTransient(error)) throw error
                lastFailure = error
                if (attempt < attempts - 1) {
                    Thread.sleep(500L * (attempt + 1))
                }
            }
        }

        Assume.assumeNoException(
            "Live YouTube/Piped request failed after $attempts attempts",
            lastFailure
        )
    }

    internal fun isTransient(error: Throwable): Boolean {
        generateSequence(error) { it.cause }.forEach { current ->
            when (current) {
                is IOException,
                is TimeoutException,
                is UnresolvedAddressException -> return true
            }

            val name = current::class.qualifiedName.orEmpty()
            if (
                name.contains("UnresolvedAddress", ignoreCase = true) ||
                name.contains("ConnectTimeout", ignoreCase = true) ||
                name.contains("SocketTimeout", ignoreCase = true) ||
                name.contains("HttpRequestTimeout", ignoreCase = true)
            ) {
                return true
            }
        }

        return false
    }
}
