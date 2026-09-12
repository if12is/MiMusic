package it.vfsfitvnm.innertube.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PlayerLog {
    private val lock = Any()
    private val lines = ArrayDeque<String>(128)
    private val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Volatile
    var lastSummary: String = ""
        private set

    fun append(message: String) {
        val line = "${time.format(Date())} $message"
        synchronized(lock) {
            if (lines.size >= 120) {
                lines.removeFirst()
            }
            lines.addLast(line)
            lastSummary = message
        }
    }

    fun snapshot(): String = synchronized(lock) {
        lines.joinToString("\n")
    }

    fun clear() = synchronized(lock) {
        lines.clear()
        lastSummary = ""
    }
}
