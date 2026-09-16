package it.vfsfitvnm.vimusic.service

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import it.vfsfitvnm.vimusic.utils.bassBoostKey
import it.vfsfitvnm.vimusic.utils.equalizerEnabledKey
import it.vfsfitvnm.vimusic.utils.equalizerPresetKey
import it.vfsfitvnm.vimusic.utils.preferences

class InAppAudioFx(private val service: PlayerService) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var reverb: PresetReverb? = null

    fun apply(audioSessionId: Int) {
        release()
        val preferences = service.preferences
        if (preferences.getBoolean(equalizerEnabledKey, false)) {
            runCatching {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = true
                    val preset = preferences.getInt(equalizerPresetKey, 0)
                    if (preset in 0 until numberOfPresets) {
                        usePreset(preset.toShort())
                    }
                }
            }
        }
        if (preferences.getBoolean(bassBoostKey, false)) {
            runCatching {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = true
                    setStrength(700.toShort())
                }
            }
        }
        runCatching {
            reverb = PresetReverb(0, audioSessionId).apply { enabled = false }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { reverb?.release() }
        equalizer = null
        bassBoost = null
        reverb = null
    }
}
