package it.vfsfitvnm.vimusic.service

import it.vfsfitvnm.vimusic.utils.TimerJob
import it.vfsfitvnm.vimusic.utils.timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

object SleepTimerClock {
    fun remainingAfterTick(current: Long?, elapsedMs: Long = 1000L): Long? {
        return current?.minus(elapsedMs)?.takeIf { it > 0L }
    }
}

class SleepTimer(private val scope: CoroutineScope) {
    private var job: TimerJob? = null

    val millisLeft: StateFlow<Long?>?
        get() = job?.millisLeft

    fun start(delayMillis: Long, onFinished: () -> Unit) {
        job?.cancel()
        job = scope.timer(delayMillis, onFinished)
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
