package it.vfsfitvnm.vimusic.utils

const val audioQualityKey = "audioQuality"
const val crossfadeEnabledKey = "crossfadeEnabled"
const val bassBoostKey = "bassBoost"
const val equalizerEnabledKey = "equalizerEnabled"
const val equalizerPresetKey = "equalizerPreset"
const val wifiOnlyDownloadKey = "wifiOnlyDownload"
const val offlineModeKey = "offlineMode"
const val carModeKey = "carMode"
const val lyricsScaleKey = "lyricsScale"
const val appLockKey = "appLock"
const val hideFromRecentsKey = "hideFromRecents"
const val onboardingDoneKey = "onboardingDone"
const val onboardingStepKey = "onboardingStep"
const val smartShuffleKey = "smartShuffle"
const val khatmaMediaIdKey = "khatmaMediaId"
const val khatmaPositionKey = "khatmaPosition"
const val loopAKey = "loopA"
const val loopBKey = "loopB"
const val playbackPitchKey = "playbackPitch"
const val lowPowerModeKey = "lowPowerMode"
const val chargingOnlyDownloadKey = "chargingOnlyDownload"
const val pipOnLeaveKey = "pipOnLeave"
const val lastPlayedMediaIdKey = "lastPlayedMediaId"
const val lastPlayedPositionKey = "lastPlayedPosition"
const val videoLyricsKey = "videoLyrics"

/** false = audio only (default), true = play the music video when the source has one. */
const val videoModeKey = "videoMode"

/**
 * 0.7.9 saved video mode, then crashed on the main thread for every later song.
 * The next launch plays songs as sound again. The video mark still opens the picture.
 */
const val videoModeRecoveredKey = "videoModeRecovered0710"
const val youtubeCookieKey = "youtubeCookie"
const val backupTreeUriKey = "backupTreeUri"
fun playlistCoverKey(playlistId: Long) = "playlistCover_$playlistId"
