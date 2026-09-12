package it.vfsfitvnm.innertube.utils

import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.MusicCarouselShelfRenderer
import it.vfsfitvnm.innertube.models.NextResponse
import it.vfsfitvnm.innertube.models.SectionListRenderer

private val relatedTabTitles = listOf(
    "related",
    "محتوى مشابه",
    "محتوي مشابه",
    "ذات صلة",
    "ذات الصله"
)

private val songSectionTitles = listOf(
    "You might also like",
    "أغانٍ قد تعجبك أيضًا",
    "أغان قد تعجبك أيضا",
    "أغاني قد تعجبك أيضًا",
    "Songs you might like"
)

private val playlistSectionTitles = listOf(
    "Recommended playlists",
    "قوائم التشغيل المقترحة",
    "قوائم تشغيل مقترحة"
)

private val artistSectionTitles = listOf(
    "Similar artists",
    "فنّانون مماثلون",
    "فنانون مماثلون",
    "فنانون مشابهون"
)

private val albumStraplines = listOf(
    "MORE FROM",
    "المزيد من أعمال",
    "المزيد من اعمال",
    "More from"
)

internal fun relatedBrowseId(
    tabs: List<NextResponse.Contents.SingleColumnMusicWatchNextResultsRenderer.TabbedRenderer.WatchNextTabbedResultsRenderer.Tab>?
): String? {
    if (tabs.isNullOrEmpty()) return null

    tabs.forEach { tab ->
        val renderer = tab.tabRenderer ?: return@forEach
        val browseId = renderer.endpoint?.browseEndpoint?.browseId
        if (browseId?.startsWith("MPTR") == true) {
            return browseId
        }

        val title = renderer.title.orEmpty()
        if (relatedTabTitles.any { alias -> title.contains(alias, ignoreCase = true) } &&
            !browseId.isNullOrBlank()
        ) {
            return browseId
        }
    }

    return tabs.asReversed().firstNotNullOfOrNull { tab ->
        tab.tabRenderer?.endpoint?.browseEndpoint?.browseId
            ?.takeIf { browseId -> !browseId.startsWith("MPLY") }
    }
}

internal fun SectionListRenderer.toRelatedPage(): Innertube.RelatedPage {
    val titledSongs = findSectionByTitle(*songSectionTitles.toTypedArray())
        ?.musicCarouselShelfRenderer
        ?.songItems()
    val titledPlaylists = findSectionByTitle(*playlistSectionTitles.toTypedArray())
        ?.musicCarouselShelfRenderer
        ?.playlistItems()
    val titledArtists = findSectionByTitle(*artistSectionTitles.toTypedArray())
        ?.musicCarouselShelfRenderer
        ?.artistItems()
    val titledAlbums = findSectionByStrapline(*albumStraplines.toTypedArray())
        ?.musicCarouselShelfRenderer
        ?.albumItems()
        .orEmpty()
        .ifEmpty {
            findSectionByTitle(*albumStraplines.toTypedArray())
                ?.musicCarouselShelfRenderer
                ?.albumItems()
                .orEmpty()
        }

    val discoveredSongs = mutableListOf<Innertube.SongItem>()
    val discoveredPlaylists = mutableListOf<Innertube.PlaylistItem>()
    val discoveredAlbums = mutableListOf<Innertube.AlbumItem>()
    val discoveredArtists = mutableListOf<Innertube.ArtistItem>()

    contents.orEmpty().forEach { content ->
        val carousel = content.musicCarouselShelfRenderer ?: return@forEach
        discoveredSongs += carousel.songItems()
        carousel.contents.orEmpty().forEach { item ->
            val renderer = item.musicTwoRowItemRenderer ?: return@forEach
            when (renderer.pageType()) {
                "MUSIC_PAGE_TYPE_ALBUM" -> Innertube.AlbumItem.from(renderer)?.let(discoveredAlbums::add)
                "MUSIC_PAGE_TYPE_ARTIST" -> Innertube.ArtistItem.from(renderer)?.let(discoveredArtists::add)
                "MUSIC_PAGE_TYPE_PLAYLIST" -> Innertube.PlaylistItem.from(renderer)?.let(discoveredPlaylists::add)
                else -> when {
                    renderer.browseId()?.startsWith("UC") == true ->
                        Innertube.ArtistItem.from(renderer)?.let(discoveredArtists::add)
                    renderer.browseId()?.startsWith("MPRE") == true ->
                        Innertube.AlbumItem.from(renderer)?.let(discoveredAlbums::add)
                    renderer.browseId()?.let { id ->
                        id.startsWith("VL") || id.startsWith("PL") || id.startsWith("OLAK")
                    } == true ->
                        Innertube.PlaylistItem.from(renderer)?.let(discoveredPlaylists::add)
                }
            }
        }
    }

    return Innertube.RelatedPage(
        songs = (titledSongs ?: discoveredSongs).distinctBy(Innertube.SongItem::key).takeIf { it.isNotEmpty() },
        playlists = (titledPlaylists ?: discoveredPlaylists)
            .distinctBy(Innertube.PlaylistItem::key)
            .sortedByDescending { it.channel?.name == "YouTube Music" }
            .takeIf { it.isNotEmpty() },
        albums = titledAlbums.ifEmpty { discoveredAlbums }
            .distinctBy(Innertube.AlbumItem::key)
            .takeIf { it.isNotEmpty() },
        artists = (titledArtists ?: discoveredArtists)
            .distinctBy(Innertube.ArtistItem::key)
            .takeIf { it.isNotEmpty() }
    )
}

private fun MusicCarouselShelfRenderer.songItems(): List<Innertube.SongItem> =
    contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
        ?.mapNotNull(Innertube.SongItem::from)
        .orEmpty()

private fun MusicCarouselShelfRenderer.playlistItems(): List<Innertube.PlaylistItem> =
    contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
        ?.mapNotNull(Innertube.PlaylistItem::from)
        .orEmpty()

private fun MusicCarouselShelfRenderer.albumItems(): List<Innertube.AlbumItem> =
    contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
        ?.mapNotNull(Innertube.AlbumItem::from)
        .orEmpty()

private fun MusicCarouselShelfRenderer.artistItems(): List<Innertube.ArtistItem> =
    contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
        ?.mapNotNull(Innertube.ArtistItem::from)
        .orEmpty()

private fun it.vfsfitvnm.innertube.models.MusicTwoRowItemRenderer.pageType(): String? =
    navigationEndpoint?.browseEndpoint?.type

private fun it.vfsfitvnm.innertube.models.MusicTwoRowItemRenderer.browseId(): String? =
    navigationEndpoint?.browseEndpoint?.browseId
