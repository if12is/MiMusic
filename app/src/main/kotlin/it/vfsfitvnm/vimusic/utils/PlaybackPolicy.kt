package it.vfsfitvnm.vimusic.utils

enum class PlaybackChoice {
    Downloaded,
    LocalCache,
    NeedNetwork,
    Unavailable
}

object PlaybackPolicy {
    fun choose(
        fullyDownloaded: Boolean,
        offlineOrNoNetwork: Boolean,
        hasPartialCache: Boolean
    ): PlaybackChoice = when {
        fullyDownloaded -> PlaybackChoice.Downloaded
        offlineOrNoNetwork && hasPartialCache -> PlaybackChoice.LocalCache
        offlineOrNoNetwork -> PlaybackChoice.Unavailable
        else -> PlaybackChoice.NeedNetwork
    }
}
