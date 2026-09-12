package it.vfsfitvnm.innertube.utils

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

internal object NewPipeSupport {
    private val lock = Any()

    @Volatile
    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            NewPipe.init(HttpDownloader(), Localization("ar"), ContentCountry("EG"))
            initialized = true
        }
    }
}

private const val BrowserUserAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

private class HttpDownloader : Downloader() {
    override fun execute(request: Request): Response {
        val connection = (URL(request.url()).openConnection() as HttpURLConnection).apply {
            requestMethod = request.httpMethod()
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 20_000
            useCaches = false
            setRequestProperty("User-Agent", BrowserUserAgent)
            setRequestProperty("Accept-Encoding", "gzip")
            request.headers().forEach { (name, values) ->
                values.forEach { value -> setRequestProperty(name, value) }
            }
            request.dataToSend()?.let { data ->
                doOutput = true
                outputStream.use { stream -> stream.write(data) }
            }
        }

        val code = connection.responseCode
        if (code == 429) {
            connection.disconnect()
            throw ReCaptchaException("reCaptcha Challenge requested", request.url())
        }

        val rawStream = if (code >= 400) connection.errorStream else connection.inputStream
        val encoding = connection.contentEncoding
        val body = rawStream?.let { stream ->
            val decoded = if (encoding.equals("gzip", ignoreCase = true)) {
                GZIPInputStream(stream)
            } else {
                stream
            }
            decoded.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val headers = connection.headerFields
            .filterKeys { it != null }
            .mapKeys { (key, _) -> key!! }
        val latestUrl = connection.url.toString()
        val message = connection.responseMessage
        connection.disconnect()

        return Response(code, message, headers, body, latestUrl)
    }
}
