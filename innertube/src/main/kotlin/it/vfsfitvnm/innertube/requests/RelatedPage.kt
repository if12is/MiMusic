package it.vfsfitvnm.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.BrowseResponse
import it.vfsfitvnm.innertube.models.NextResponse
import it.vfsfitvnm.innertube.models.SectionListRenderer
import it.vfsfitvnm.innertube.models.bodies.BrowseBody
import it.vfsfitvnm.innertube.models.bodies.NextBody
import it.vfsfitvnm.innertube.utils.relatedBrowseId
import it.vfsfitvnm.innertube.utils.runCatchingNonCancellable
import it.vfsfitvnm.innertube.utils.toRelatedPage

const val DefaultLandingVideoId = "J7p4bzqLvCw"

suspend fun Innertube.landingPage(videoId: String = DefaultLandingVideoId) = runCatchingNonCancellable {
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

internal suspend fun Innertube.homePageOrNull(): Innertube.RelatedPage? {
    val response = client.post(browse) {
        setBody(BrowseBody(browseId = "FEmusic_home"))
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
