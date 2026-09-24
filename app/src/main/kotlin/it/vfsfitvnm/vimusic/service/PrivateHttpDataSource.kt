package it.vfsfitvnm.vimusic.service

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import it.vfsfitvnm.vimusic.utils.CleartextPolicy
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Reads HTTP from a local server without opening cleartext for the rest of the internet.
 * Android's network security config cannot list a LAN address the user types at runtime.
 */
internal class PrivateHttpDataSource : BaseDataSource(true) {
    private var socket: Socket? = null
    private var input: InputStream? = null
    private var bytesRemaining = C.LENGTH_UNSET.toLong()
    private var openedUri: Uri? = null

    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        if (!CleartextPolicy.allows(uri.toString())) {
            throw DataSourceException(DataSourceException.POSITION_OUT_OF_RANGE)
        }
        val host = uri.host ?: throw DataSourceException(DataSourceException.POSITION_OUT_OF_RANGE)
        val port = if (uri.port == -1) 80 else uri.port
        val path = buildString {
            append(uri.path?.ifBlank { "/" } ?: "/")
            if (!uri.query.isNullOrBlank()) {
                append('?')
                append(uri.query)
            }
        }
        val rangeHeader = if (dataSpec.position > 0 || dataSpec.length > 0) {
            val end = if (dataSpec.length > 0) (dataSpec.position + dataSpec.length - 1).toString() else ""
            "Range: bytes=${dataSpec.position}-$end\r\n"
        } else {
            ""
        }
        val request = "GET $path HTTP/1.1\r\nHost: $host\r\nConnection: close\r\n$rangeHeader\r\n"
        val opened = Socket()
        opened.connect(InetSocketAddress(host, port), 16_000)
        opened.soTimeout = 8_000
        opened.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
        val stream = opened.getInputStream()
        val headerBytes = readHeaders(stream)
        val headerText = headerBytes.toString(Charsets.ISO_8859_1)
        val status = headerText.lineSequence().firstOrNull()
            ?.substringAfter(' ')
            ?.take(3)
            ?.toIntOrNull()
            ?: throw DataSourceException(DataSourceException.POSITION_OUT_OF_RANGE)
        if (status !in 200..299) {
            opened.close()
            throw DataSourceException(DataSourceException.POSITION_OUT_OF_RANGE)
        }
        val contentLength = headerText.lineSequence()
            .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.toLongOrNull()
        socket = opened
        input = stream
        openedUri = uri
        bytesRemaining = contentLength ?: C.LENGTH_UNSET.toLong()
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) length else minOf(length.toLong(), bytesRemaining).toInt()
        val read = input?.read(buffer, offset, toRead) ?: C.RESULT_END_OF_INPUT
        if (read == -1) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = openedUri

    override fun close() {
        input = null
        openedUri = null
        bytesRemaining = C.LENGTH_UNSET.toLong()
        socket?.close()
        socket = null
    }

    private fun readHeaders(stream: InputStream): ByteArray {
        val header = ArrayList<Byte>(256)
        var matched = 0
        val marker = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())
        while (matched < marker.size) {
            val value = stream.read()
            if (value < 0) break
            val byte = value.toByte()
            header += byte
            matched = if (byte == marker[matched]) matched + 1 else if (byte == marker[0]) 1 else 0
            if (header.size > 64 * 1024) break
        }
        return header.toByteArray()
    }
}
