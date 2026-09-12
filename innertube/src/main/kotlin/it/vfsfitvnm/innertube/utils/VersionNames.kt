package it.vfsfitvnm.innertube.utils

fun compareVersionNames(latest: String, current: String): Int {
    val latestParts = latest.trimStart('v').split('.', '-').mapNotNull { it.toIntOrNull() }
    val currentParts = current.trimStart('v').split('.', '-').mapNotNull { it.toIntOrNull() }
    val size = maxOf(latestParts.size, currentParts.size)
    for (index in 0 until size) {
        val a = latestParts.getOrElse(index) { 0 }
        val b = currentParts.getOrElse(index) { 0 }
        if (a != b) return a.compareTo(b)
    }
    return 0
}
