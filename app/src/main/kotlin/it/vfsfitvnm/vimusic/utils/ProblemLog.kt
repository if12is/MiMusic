package it.vfsfitvnm.vimusic.utils

import org.acra.ACRA

object ProblemLog {
    fun record(where: String, error: Throwable) {
        PlaybackLogStore.append("failure $where: ${error.javaClass.simpleName} ${error.message}")
        runCatching {
            if (ACRA.isInitialised) {
                ACRA.errorReporter.handleSilentException(error)
            }
        }
    }
}
