package it.vfsfitvnm.vimusic.service

object LibraryQueue {
    private val roots = setOf(
        "favorites",
        "offline",
        "history",
        "quran",
        "shuffle",
        "songs",
        "playlists",
        "albums"
    )

    fun isLibraryId(mediaId: String): Boolean {
        val kind = mediaId.substringBefore('/')
        return kind in roots
    }

    fun argument(mediaId: String): String? = mediaId.substringAfter('/', "").ifBlank { null }

    /** Plays from the chosen song through the end of the list. */
    fun fromSelection(ids: List<String>, selectedId: String?): List<String> {
        if (selectedId.isNullOrBlank()) return ids
        val index = ids.indexOf(selectedId)
        if (index <= 0) return ids
        return ids.drop(index)
    }
}
