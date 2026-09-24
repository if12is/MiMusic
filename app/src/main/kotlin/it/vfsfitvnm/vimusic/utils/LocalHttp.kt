package it.vfsfitvnm.vimusic.utils

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.URL

internal fun exchangeHttp(
    url: String,
    method: String = "GET",
    headers: Map<String, String> = emptyMap(),
    body: String? = null,
    connectTimeoutMs: Int = 15_000,
    readTimeoutMs: Int = 15_000
): String {
    if (url.startsWith("http://", ignoreCase = true) && !CleartextPolicy.allows(url)) {
        error("HTTP is only allowed for a local server you add yourself")
    }
    if (url.startsWith("http://", ignoreCase = true)) {
        return socketExchange(url, method, headers, body, connectTimeoutMs, readTimeoutMs)
    }
    return urlConnectionExchange(url, method, headers, body, connectTimeoutMs, readTimeoutMs)
}

private fun urlConnectionExchange(
    url: String,
    method: String,
    headers: Map<String, String>,
    body: String?,
    connectTimeoutMs: Int,
    readTimeoutMs: Int
): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = connectTimeoutMs
        readTimeout = readTimeoutMs
        doOutput = body != null
        headers.forEach { (name, value) -> setRequestProperty(name, value) }
    }
    if (body != null) {
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    }
    val stream = if (connection.responseCode >= 400) connection.errorStream else connection.inputStream
    val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    val code = connection.responseCode
    connection.disconnect()
    if (code !in 200..299) error("HTTP $code")
    return text
}

private fun socketExchange(
    url: String,
    method: String,
    headers: Map<String, String>,
    body: String?,
    connectTimeoutMs: Int,
    readTimeoutMs: Int
): String {
    val uri = URI(url)
    val host = uri.host ?: error("Missing host")
    val port = if (uri.port == -1) 80 else uri.port
    val path = buildString {
        append(uri.rawPath?.ifBlank { "/" } ?: "/")
        if (!uri.rawQuery.isNullOrBlank()) {
            append('?')
            append(uri.rawQuery)
        }
    }
    val payload = body?.toByteArray(Charsets.UTF_8)
    val request = buildString {
        append(method.uppercase())
        append(' ')
        append(path)
        append(" HTTP/1.1\r\nHost: ")
        append(if (port == 80) host else "$host:$port")
        append("\r\nConnection: close\r\n")
        headers.forEach { (name, value) ->
            append(name)
            append(": ")
            append(value)
            append("\r\n")
        }
        if (payload != null) {
            append("Content-Length: ")
            append(payload.size)
            append("\r\n")
        }
        append("\r\n")
    }
    Socket().use { socket ->
        socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
        socket.soTimeout = readTimeoutMs
        socket.getOutputStream().use { output ->
            output.write(request.toByteArray(Charsets.US_ASCII))
            if (payload != null) output.write(payload)
            output.flush()
        }
        val raw = ByteArrayOutputStream()
        val input = BufferedInputStream(socket.getInputStream())
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            raw.write(buffer, 0, read)
        }
        val bytes = raw.toByteArray()
        val split = indexOfHeaderEnd(bytes)
        if (split < 0) error("HTTP response had no headers")
        val headerText = bytes.copyOfRange(0, split).toString(Charsets.ISO_8859_1)
        val status = headerText.lineSequence().firstOrNull()?.substringAfter(' ')?.take(3)?.toIntOrNull()
            ?: error("HTTP status missing")
        if (status !in 200..299) error("HTTP $status")
        return bytes.copyOfRange(split + 4, bytes.size).toString(Charsets.UTF_8)
    }
}

private fun indexOfHeaderEnd(bytes: ByteArray): Int {
    for (index in 0 until bytes.size - 3) {
        if (bytes[index] == '\r'.code.toByte() &&
            bytes[index + 1] == '\n'.code.toByte() &&
            bytes[index + 2] == '\r'.code.toByte() &&
            bytes[index + 3] == '\n'.code.toByte()
        ) {
            return index
        }
    }
    return -1
}
