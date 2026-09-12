package it.vfsfitvnm.vimusic.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import it.vfsfitvnm.innertube.utils.PlayerLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PlaybackLogStore {
    const val TAG = "MiMusicPlayback"

    private val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    @Volatile
    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.filesDir, "playback.log")
    }

    fun append(message: String) {
        val line = "${time.format(Date())} $message"
        Log.i(TAG, message)
        PlayerLog.append(message)
        runCatching {
            val file = logFile ?: return
            file.appendText(line + "\n")
            if (file.length() > 64 * 1024) {
                val trimmed = file.readLines().takeLast(80).joinToString("\n") + "\n"
                file.writeText(trimmed)
            }
        }
    }

    fun snapshot(): String {
        val fileText = runCatching { logFile?.takeIf { it.exists() }?.readText() }.getOrNull()
        return buildString {
            if (!fileText.isNullOrBlank()) {
                appendLine(fileText.trim())
            }
            val memory = PlayerLog.snapshot()
            if (memory.isNotBlank() && memory !in this) {
                appendLine(memory)
            }
        }.trim()
    }

    fun lastSummary(): String = PlayerLog.lastSummary

    fun shareIntent(): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MiMusic playback log")
            putExtra(Intent.EXTRA_TEXT, snapshot().ifBlank { "No playback log yet." })
        }
    }
}
