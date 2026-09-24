package it.vfsfitvnm.vimusic.utils

fun newestBackupName(names: List<String>): String? =
    names
        .filter { it.startsWith("mimusic-") && it.endsWith(".db") }
        .maxOrNull()
