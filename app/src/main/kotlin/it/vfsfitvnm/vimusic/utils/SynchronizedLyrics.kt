package it.vfsfitvnm.vimusic.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.vfsfitvnm.kugou.LyricLine
import it.vfsfitvnm.kugou.LyricWord

class SynchronizedLyrics(
    val lines: List<LyricLine>,
    private val positionProvider: () -> Long
) {
    val sentences: List<Pair<Long, String>>
        get() = lines.map { it.timeMs to it.text }

    var index by mutableStateOf(currentIndex)
        private set

    var wordIndex by mutableStateOf(currentWordIndex)
        private set

    val currentLine: LyricLine?
        get() = lines.getOrNull(index)

    val currentWords: List<LyricWord>
        get() = currentLine?.words.orEmpty()

    private val currentIndex: Int
        get() {
            val position = positionProvider()
            var next = 0
            for (item in lines) {
                if (item.timeMs > position) break
                next++
            }
            return (next - 1).coerceAtLeast(0)
        }

    private val currentWordIndex: Int
        get() {
            val words = currentWords
            if (words.isEmpty()) return -1
            val position = positionProvider()
            var next = 0
            for (word in words) {
                if (word.timeMs > position) break
                next++
            }
            return (next - 1).coerceAtLeast(0)
        }

    fun update(): Boolean {
        val newIndex = currentIndex
        val newWordIndex = currentWordIndex
        val changed = newIndex != index || newWordIndex != wordIndex
        index = newIndex
        wordIndex = newWordIndex
        return changed
    }
}
