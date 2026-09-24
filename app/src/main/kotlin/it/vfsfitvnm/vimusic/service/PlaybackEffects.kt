package it.vfsfitvnm.vimusic.service

import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import it.vfsfitvnm.vimusic.utils.BassLevel
import it.vfsfitvnm.vimusic.utils.EqualizerPreset
import it.vfsfitvnm.vimusic.utils.bandGains
import it.vfsfitvnm.vimusic.utils.bassBoostKey
import it.vfsfitvnm.vimusic.utils.equalizerEnabledKey
import it.vfsfitvnm.vimusic.utils.equalizerPresetKey
import it.vfsfitvnm.vimusic.utils.getEnum

/**
 * In-app equalizer and bass boost on the player audio session.
 * Both stay released while their settings are off, so they cannot change volume by accident.
 */
class PlaybackEffects private constructor(
    private val equalizer: Equalizer?,
    private val bassBoost: BassBoost?
) {
    fun release() {
        equalizer?.let { effect ->
            effect.enabled = false
            effect.release()
        }
        bassBoost?.let { effect ->
            effect.enabled = false
            effect.release()
        }
    }

    companion object {
        fun attach(sessionId: Int, preferences: SharedPreferences): PlaybackEffects? {
            if (sessionId == 0) return null
            val preset = preferences.getEnum(equalizerPresetKey, EqualizerPreset.Flat)
            val bass = preferences.getEnum(bassBoostKey, BassLevel.Off)
            val equalizerOn = preferences.getBoolean(equalizerEnabledKey, false)
            val bassOn = bass != BassLevel.Off
            if (!equalizerOn && !bassOn) return null

            val equalizer = if (equalizerOn) openEqualizer(sessionId, preset) else null
            val bassEffect = if (bassOn) openBass(sessionId, bass.strength) else null
            if (equalizer == null && bassEffect == null) return null
            return PlaybackEffects(equalizer, bassEffect)
        }

        private fun openEqualizer(sessionId: Int, preset: EqualizerPreset): Equalizer? =
            try {
                Equalizer(0, sessionId).apply {
                    val range = bandLevelRange
                    val gains = preset.bandGains(
                        bandCount = numberOfBands.toInt(),
                        minLevel = range[0].toInt(),
                        maxLevel = range[1].toInt()
                    )
                    gains.forEachIndexed { index, level ->
                        setBandLevel(index.toShort(), level)
                    }
                    enabled = true
                }
            } catch (_: RuntimeException) {
                null
            } catch (_: UnsupportedOperationException) {
                null
            }

        private fun openBass(sessionId: Int, strength: Short): BassBoost? =
            try {
                BassBoost(0, sessionId).apply {
                    setStrength(strength)
                    enabled = true
                }
            } catch (_: RuntimeException) {
                null
            } catch (_: UnsupportedOperationException) {
                null
            }
    }
}
