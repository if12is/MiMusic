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

suspend fun Innertube.landingPage(videoId: String = DefaultLandingVideoId) = runCatchingNonCancellable {
    if (Context.hl.startsWith("ar", ignoreCase = true)) {
        val arabic = arabicLandingPage(videoId)
        if (arabic != null && !arabic.isEmpty) {
            return@runCatchingNonCancellable arabic
        }
    }

    val related = runCatching { relatedPageOrNull(videoId) }.getOrNull()
    if (related != null && !related.isEmpty) {
        return@runCatchingNonCancellable related
    }

    val home = homePageOrNull()
    if (home != null && !home.isEmpty) {
        return@runCatchingNonCancellable home
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
