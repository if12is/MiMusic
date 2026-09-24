package it.vfsfitvnm.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.BrowseResponse
import it.vfsfitvnm.innertube.models.Context
import it.vfsfitvnm.innertube.models.NextResponse
import it.vfsfitvnm.innertube.models.SectionListRenderer
import it.vfsfitvnm.innertube.models.bodies.BrowseBody
import it.vfsfitvnm.innertube.models.bodies.NextBody
import it.vfsfitvnm.innertube.models.bodies.SearchBody
import it.vfsfitvnm.innertube.utils.from
import it.vfsfitvnm.innertube.utils.relatedBrowseId
import it.vfsfitvnm.innertube.utils.runCatchingNonCancellable
import it.vfsfitvnm.innertube.utils.toRelatedPage

const val DefaultLandingVideoId = "J7p4bzqLvCw"
private const val ArabicHitsQuery = "أغاني عربية"

val ArabRegions = setOf(
    "EG", "SA", "AE", "KW", "QA", "BH", "OM", "YE", "IQ", "SY", "JO", "LB",
    "PS", "LY", "TN", "DZ", "MA", "SD", "MR", "SO", "DJ", "KM"
)

suspend fun Innertube.landingPage(videoId: String = DefaultLandingVideoId) = runCatchingNonCancellable {
    // Recommendations follow the listener's region (Context.gl), not the UI language:
    // someone in the US using the Arabic UI should not get Egyptian hits by default.
    if (Context.gl.uppercase() in ArabRegions) {
        val arabic = arabicLandingPage(videoId)
        if (arabic != null && !arabic.isEmpty) {
            return@runCatchingNonCancellable arabic
        }
    }

    // YouTube Music's own home is localized by gl, so prefer it outside the Arab world;
    // the default seed video is an Arabic song and would skew "related" results.
    val home = runCatching { homePageOrNull() }.getOrNull()
    if (home != null && !home.isEmpty) {
        return@runCatchingNonCancellable home
    }

    val related = runCatching { relatedPageOrNull(videoId) }.getOrNull()
    if (related != null && !related.isEmpty) {
        return@runCatchingNonCancellable related
    }

    related ?: home ?: error("Unable to load home recommendations")
}

suspend fun Innertube.relatedPage(body: NextBody) = runCatchingNonCancellable {
    relatedPageOrNull(body.videoId) ?: homePageOrNull()
}

private suspend fun Innertube.arabicLandingPage(videoId: String?): Innertube.RelatedPage? {
    val songs = runCatching {
        searchPage(
            body = SearchBody(
                query = ArabicHitsQuery,
                params = Innertube.SearchFilter.Song.value
            ),
            fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
        )?.getOrNull()?.items
    }.getOrNull()

    val playlists = runCatching {
        searchPage(
            body = SearchBody(
                query = "قوائم تشغيل عربية",
                params = Innertube.SearchFilter.FeaturedPlaylist.value
            ),
            fromMusicShelfRendererContent = Innertube.PlaylistItem.Companion::from
        )?.getOrNull()?.items
    }.getOrNull()

    val artists = runCatching {
        searchPage(
            body = SearchBody(
                query = "فنانون عرب",
                params = Innertube.SearchFilter.Artist.value
            ),
            fromMusicShelfRendererContent = Innertube.ArtistItem.Companion::from
        )?.getOrNull()?.items
    }.getOrNull()

    val albums = runCatching {
        searchPage(
            body = SearchBody(
                query = "ألبومات عربية",
                params = Innertube.SearchFilter.Album.value
            ),
            fromMusicShelfRendererContent = Innertube.AlbumItem.Companion::from
        )?.getOrNull()?.items
    }.getOrNull()

    val explore = runCatching { browsePageOrNull("FEmusic_explore") }.getOrNull()
    val home = runCatching { homePageOrNull() }.getOrNull()
    val related = videoId?.let { id -> runCatching { relatedPageOrNull(id) }.getOrNull() }

    val page = Innertube.RelatedPage(
        songs = songs.takeUnless { it.isNullOrEmpty() }
            ?: related?.songs
            ?: home?.songs
            ?: explore?.songs,
        playlists = playlists.takeUnless { it.isNullOrEmpty() }
            ?: explore?.playlists
            ?: home?.playlists
            ?: related?.playlists,
        albums = albums.takeUnless { it.isNullOrEmpty() }
            ?: explore?.albums
            ?: home?.albums
            ?: related?.albums,
        artists = artists.takeUnless { it.isNullOrEmpty() }
            ?: explore?.artists
            ?: home?.artists
            ?: related?.artists
    )

    return page.takeUnless { it.isEmpty }
}

private suspend fun Innertube.relatedPageOrNull(videoId: String?): Innertube.RelatedPage? {
    if (videoId.isNullOrBlank()) return null

    val nextResponse = client.post(next) {
        setBody(NextBody(videoId = videoId))
    }.body<NextResponse>()

    val browseId = relatedBrowseId(
        nextResponse
            .contents
            ?.singleColumnMusicWatchNextResultsRenderer
            ?.tabbedRenderer
            ?.watchNextTabbedResultsRenderer
            ?.tabs
    ) ?: return null

    val response = client.post(browse) {
        setBody(BrowseBody(browseId = browseId))
    }.body<BrowseResponse>()

    return response.sectionListRenderer()?.toRelatedPage()?.takeUnless { it.isEmpty }
}

internal suspend fun Innertube.homePageOrNull(): Innertube.RelatedPage? =
    browsePageOrNull("FEmusic_home")

private suspend fun Innertube.browsePageOrNull(browseId: String): Innertube.RelatedPage? {
    val response = client.post(browse) {
        setBody(BrowseBody(browseId = browseId))
    }.body<BrowseResponse>()

    return response.sectionListRenderer()?.toRelatedPage()?.takeUnless { it.isEmpty }
}

private fun BrowseResponse.sectionListRenderer(): SectionListRenderer? =
    contents?.sectionListRenderer
        ?: contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
