package it.vfsfitvnm.vimusic.service

import android.content.Context
import android.net.Uri
import it.vfsfitvnm.vimusic.utils.PlaybackLogStore
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicReference

/**
 * Serves one on-device file to a Chromecast on the same network.
 * The socket is not loopback. It is stopped when the cast session ends.
 */
internal class LocalCastServer(private val context: Context) {
    private val socketRef = AtomicReference<ServerSocket?>(null)
    private var worker: Thread? = null
    private var served: File? = null
    private var temporary = false
    private var contentType = "application/octet-stream"

    fun urlFor(uri: Uri): String? {
        stop()
        val prepared = prepare(uri) ?: return null
        val address = lanAddress() ?: return null
        val server = try {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", 0))
            }
        } catch (e: IOException) {
            PlaybackLogStore.append("cast server bind ${e.message}")
            if (prepared.temporary) prepared.file.delete()
            return null
        }
        served = prepared.file
        temporary = prepared.temporary
        contentType = prepared.mime
        socketRef.set(server)
        worker = Thread({ acceptLoop(server, prepared.file) }, "local-cast").apply {
            isDaemon = true
            start()
        }
        return "http://$address:${server.localPort}/track"
    }

    fun stop() {
        try {
            socketRef.getAndSet(null)?.close()
        } catch (e: IOException) {
            PlaybackLogStore.append("cast server stop ${e.message}")
        }
        worker = null
        if (temporary) served?.delete()
        served = null
        temporary = false
    }

    private fun acceptLoop(server: ServerSocket, source: File) {
        while (!server.isClosed) {
            val client = try {
                server.accept()
            } catch (e: SocketException) {
                break
            } catch (e: IOException) {
                PlaybackLogStore.append("cast accept ${e.message}")
                break
            }
            try {
                serve(client, source)
            } catch (e: IOException) {
                PlaybackLogStore.append("cast serve ${e.message}")
            } finally {
                try {
                    client.close()
                } catch (e: IOException) {
                    PlaybackLogStore.append("cast close ${e.message}")
                }
            }
        }
    }

    private fun serve(client: Socket, source: File) {
        client.soTimeout = 10_000
        val header = client.getInputStream().bufferedReader()
        val request = header.readLine() ?: return
        var rangeHeader: String? = null
        while (true) {
            val line = header.readLine() ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Range:", ignoreCase = true)) {
                rangeHeader = line.substringAfter(':').trim()
            }
        }
        val path = request.split(' ').getOrNull(1).orEmpty()
        val output = client.getOutputStream()
        if (!path.startsWith("/track") || source.length() <= 0L) {
            output.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
            return
        }
        val range = CastRanges.bounds(rangeHeader, source.length())
        val count = range.last - range.first + 1
        val partial = rangeHeader != null
        val status = if (partial) "206 Partial Content" else "200 OK"
        val head = buildString {
            append("HTTP/1.1 ").append(status).append("\r\n")
            append("Content-Type: ").append(contentType).append("\r\n")
            append("Accept-Ranges: bytes\r\n")
            append("Content-Length: ").append(count).append("\r\n")
            if (partial) {
                append("Content-Range: bytes ")
                append(range.first).append('-').append(range.last).append('/').append(source.length())
                append("\r\n")
            }
            append("Connection: close\r\n\r\n")
        }
        output.write(head.toByteArray(Charsets.US_ASCII))
        FileInputStream(source).use { file ->
            file.channel.position(range.first)
            val buffer = ByteArray(16 * 1024)
            var remaining = count
            while (remaining > 0) {
                val read = file.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read < 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
        }
        output.flush()
    }

    private fun prepare(uri: Uri): Prepared? {
        val scheme = uri.scheme?.lowercase()
        if (scheme == "file" || scheme == null) {
            val file = uri.path?.let(::File) ?: return null
            if (!file.isFile || !file.canRead()) return null
            return Prepared(file, temporary = false, mime = mimeFor(file.name, null))
        }
        if (scheme != "content") return null
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val out = File(context.cacheDir, "cast-share.bin")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
        } catch (e: IOException) {
            PlaybackLogStore.append("cast copy ${e.message}")
            return null
        } catch (e: SecurityException) {
            PlaybackLogStore.append("cast copy ${e.message}")
            return null
        }
        if (!out.isFile || out.length() <= 0L) return null
        return Prepared(out, temporary = true, mime = mimeFor(uri.lastPathSegment.orEmpty(), mime))
    }

    private fun mimeFor(name: String, resolved: String?): String {
        if (!resolved.isNullOrBlank()) return resolved
        return when (name.substringAfterLast('.', "").lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "flac" -> "audio/flac"
            "ogg", "opus" -> "audio/ogg"
            "wav" -> "audio/wav"
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            else -> "application/octet-stream"
        }
    }

    private fun lanAddress(): String? {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces() ?: return null
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            val name = intf.name.lowercase()
            if (name.startsWith("dummy") || name.startsWith("docker") || name.startsWith("tun")) continue
            for (addr in intf.inetAddresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }

    private data class Prepared(val file: File, val temporary: Boolean, val mime: String)
}
