package it.vfsfitvnm.vimusic.utils

import kotlin.math.abs

enum class EqualizerPreset {
    Flat,
    Bass,
    Vocal,
    Treble
}

enum class BassLevel(val strength: Short) {
    Off(0),
    Low(300),
    Medium(600),
    High(900)
}

private const val SafeBoostMillibels = 600

/**
 * Band offsets in millibels. The boost is capped so an in-app preset cannot
 * slam the stream volume the way an uncapped effect does.
 */
fun EqualizerPreset.bandGains(bandCount: Int, minLevel: Int, maxLevel: Int): ShortArray {
    if (bandCount <= 0) return ShortArray(0)
    val last = bandCount - 1
    val shape = FloatArray(bandCount) { index ->
        when (this) {
            EqualizerPreset.Flat -> 0f
            EqualizerPreset.Bass -> if (index <= last / 3) 1f else if (index >= last * 2 / 3) -0.25f else 0.15f
            EqualizerPreset.Vocal -> {
                val mid = last / 2f
                val distance = if (last == 0) 0f else abs(index - mid) / last
                (1f - distance * 2f).coerceIn(-0.2f, 1f)
            }
            EqualizerPreset.Treble -> if (index >= last * 2 / 3) 1f else if (index <= last / 3) -0.2f else 0.2f
        }
    }
    val room = (maxLevel - minLevel).coerceAtLeast(0) / 4
    val boost = SafeBoostMillibels.coerceAtMost(if (room == 0) SafeBoostMillibels else room)
    return ShortArray(bandCount) { index ->
        (shape[index] * boost).toInt().coerceIn(minLevel, maxLevel).toShort()
    }
}
