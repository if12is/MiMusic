package it.vfsfitvnm.vimusic.utils

data class PlannedMix(
    val id: String,
    val songIds: List<String>
)

/**
 * Three mixes from songs already on the device. The order changes with [epochDay]
 * and does not call the network.
 */
fun planDailyMixes(songs: List<Pair<String, String>>, epochDay: Long): List<PlannedMix> {
    val unique = songs
        .filter { it.first.isNotBlank() }
        .distinctBy { it.first }
    if (unique.isEmpty()) return emptyList()

    val ordered = unique.sortedWith(compareBy({ stableRank(it.second, epochDay) }, { stableRank(it.first, epochDay) }))
    val buckets = List(3) { mutableListOf<String>() }
    val lastArtist = Array(3) { "" }
    ordered.forEach { (id, artist) ->
        val bucket = (0 until 3).minBy { index ->
            val sameArtist = if (artist.isNotBlank() && artist == lastArtist[index]) 1 else 0
            sameArtist * 1000 + buckets[index].size
        }
        if (buckets[bucket].size < 25) {
            buckets[bucket] += id
            if (artist.isNotBlank()) lastArtist[bucket] = artist
        }
    }
    return buckets.mapIndexedNotNull { index, ids ->
        ids.takeIf { it.isNotEmpty() }?.let { PlannedMix("daily-$index", it) }
    }
}

private fun stableRank(value: String, epochDay: Long): Int {
    var hash = epochDay.toInt()
    value.forEach { char -> hash = 31 * hash + char.code }
    return hash
}
