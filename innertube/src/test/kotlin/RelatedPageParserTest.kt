package it.vfsfitvnm.innertube

import it.vfsfitvnm.innertube.models.MusicCarouselShelfRenderer
import it.vfsfitvnm.innertube.models.MusicResponsiveListItemRenderer
import it.vfsfitvnm.innertube.models.MusicTwoRowItemRenderer
import it.vfsfitvnm.innertube.models.NavigationEndpoint
import it.vfsfitvnm.innertube.models.NextResponse
import it.vfsfitvnm.innertube.models.Runs
import it.vfsfitvnm.innertube.models.SectionListRenderer
import it.vfsfitvnm.innertube.models.Thumbnail
import it.vfsfitvnm.innertube.models.ThumbnailRenderer
import it.vfsfitvnm.innertube.utils.lyricsBrowseId
import it.vfsfitvnm.innertube.utils.relatedBrowseId
import it.vfsfitvnm.innertube.utils.toRelatedPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedPageParserTest {
    @Test
    fun findsArabicLyricsTabByTitle() {
        val browseId = lyricsBrowseId(
            listOf(
                tab("التالي", browseId = null),
                tab("كلمات الأغنية", browseId = "MPLYt_lyrics"),
                tab("التعليقات", browseId = null),
                tab("محتوى مشابه", browseId = "MPTRt_related")
            )
        )

        assertEquals("MPLYt_lyrics", browseId)
    }

    @Test
    fun ignoresNonLyricsTabAtLegacyIndex() {
        val browseId = lyricsBrowseId(
            listOf(
                tab("التالي", browseId = null),
                tab("التعليقات", browseId = "MPLy_comments"),
                tab("محتوى مشابه", browseId = "MPTRt_related")
            )
        )

        assertEquals(null, browseId)
    }

    @Test
    fun findsRelatedTabAfterCommentsInArabic() {
        val browseId = relatedBrowseId(
            listOf(
                tab("التالي", browseId = null),
                tab("كلمات الأغنية", browseId = "MPLYt_lyrics"),
                tab("التعليقات", browseId = null),
                tab("محتوى مشابه", browseId = "MPTRt_related")
            )
        )

        assertEquals("MPTRt_related", browseId)
    }

    @Test
    fun findsRelatedTabAfterCommentsInEnglish() {
        val browseId = relatedBrowseId(
            listOf(
                tab("Up next", browseId = null),
                tab("Lyrics", browseId = "MPLYt_lyrics"),
                tab("Comments", browseId = null),
                tab("Related", browseId = "MPTRt_related")
            )
        )

        assertEquals("MPTRt_related", browseId)
    }

    @Test
    fun ignoresCommentsTabAtLegacyIndex() {
        val browseId = relatedBrowseId(
            listOf(
                tab("Up next", browseId = null),
                tab("Lyrics", browseId = "MPLYt_lyrics"),
                tab("Comments", browseId = null)
            )
        )

        assertEquals(null, browseId)
    }

    @Test
    fun parsesArabicRelatedSections() {
        val page = SectionListRenderer(
            contents = listOf(
                songCarousel("أغانٍ قد تعجبك أيضًا", "song1"),
                playlistCarousel("قوائم التشغيل المقترحة", "VLplaylist1"),
                artistCarousel("فنّانون مماثلون", "UCartist"),
                albumCarousel("The Weeknd", "MPREb_album", strapline = "المزيد من أعمال")
            ),
            continuations = null
        ).toRelatedPage()

        assertFalse(page.isEmpty)
        assertEquals("song1", page.songs?.single()?.key)
        assertEquals("VLplaylist1", page.playlists?.single()?.key)
        assertEquals("UCartist", page.artists?.single()?.key)
        assertEquals("MPREb_album", page.albums?.single()?.key)
    }

    @Test
    fun classifiesUntitledHomeCarouselsByPageType() {
        val page = SectionListRenderer(
            contents = listOf(
                untitledPlaylistCarousel("VLchart")
            ),
            continuations = null
        ).toRelatedPage()

        assertEquals("VLchart", page.playlists?.single()?.key)
        assertTrue(page.songs.isNullOrEmpty())
    }

    private fun tab(title: String, browseId: String?) =
        NextResponse.Contents.SingleColumnMusicWatchNextResultsRenderer.TabbedRenderer.WatchNextTabbedResultsRenderer.Tab(
            tabRenderer = NextResponse.Contents.SingleColumnMusicWatchNextResultsRenderer.TabbedRenderer.WatchNextTabbedResultsRenderer.Tab.TabRenderer(
                content = null,
                endpoint = browseId?.let {
                    NavigationEndpoint(
                        watchEndpoint = null,
                        watchPlaylistEndpoint = null,
                        browseEndpoint = NavigationEndpoint.Endpoint.Browse(browseId = it),
                        searchEndpoint = null
                    )
                },
                title = title
            )
        )

    private fun songCarousel(title: String, videoId: String) = SectionListRenderer.Content(
        musicShelfRenderer = null,
        gridRenderer = null,
        musicDescriptionShelfRenderer = null,
        musicCarouselShelfRenderer = MusicCarouselShelfRenderer(
            header = header(title),
            contents = listOf(
                MusicCarouselShelfRenderer.Content(
                    musicTwoRowItemRenderer = null,
                    musicResponsiveListItemRenderer = MusicResponsiveListItemRenderer(
                        fixedColumns = null,
                        flexColumns = listOf(
                            MusicResponsiveListItemRenderer.FlexColumn(
                                musicResponsiveListItemFlexColumnRenderer = MusicResponsiveListItemRenderer.FlexColumn.MusicResponsiveListItemFlexColumnRenderer(
                                    text = Runs(
                                        runs = listOf(
                                            Runs.Run(
                                                text = "Song",
                                                navigationEndpoint = watchEndpoint(videoId)
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        thumbnail = thumbnail(),
                        navigationEndpoint = watchEndpoint(videoId)
                    )
                )
            )
        )
    )

    private fun playlistCarousel(title: String, browseId: String) = twoRowCarousel(
        title = title,
        browseId = browseId,
        pageType = "MUSIC_PAGE_TYPE_PLAYLIST"
    )

    private fun artistCarousel(title: String, browseId: String) = twoRowCarousel(
        title = title,
        browseId = browseId,
        pageType = "MUSIC_PAGE_TYPE_ARTIST"
    )

    private fun albumCarousel(title: String, browseId: String, strapline: String) = twoRowCarousel(
        title = title,
        browseId = browseId,
        pageType = "MUSIC_PAGE_TYPE_ALBUM",
        strapline = strapline
    )

    private fun untitledPlaylistCarousel(browseId: String) = twoRowCarousel(
        title = "أنجح الأغاني اليوم",
        browseId = browseId,
        pageType = "MUSIC_PAGE_TYPE_PLAYLIST"
    )

    private fun twoRowCarousel(
        title: String,
        browseId: String,
        pageType: String,
        strapline: String? = null
    ) = SectionListRenderer.Content(
        musicShelfRenderer = null,
        gridRenderer = null,
        musicDescriptionShelfRenderer = null,
        musicCarouselShelfRenderer = MusicCarouselShelfRenderer(
            header = header(title, strapline),
            contents = listOf(
                MusicCarouselShelfRenderer.Content(
                    musicTwoRowItemRenderer = MusicTwoRowItemRenderer(
                        navigationEndpoint = browseEndpoint(browseId, pageType),
                        thumbnailRenderer = thumbnail(),
                        title = Runs(runs = listOf(Runs.Run(text = title, navigationEndpoint = browseEndpoint(browseId, pageType)))),
                        subtitle = null
                    ),
                    musicResponsiveListItemRenderer = null
                )
            )
        )
    )

    private fun header(title: String, strapline: String? = null) = MusicCarouselShelfRenderer.Header(
        musicTwoRowItemRenderer = null,
        musicResponsiveListItemRenderer = null,
        musicCarouselShelfBasicHeaderRenderer = MusicCarouselShelfRenderer.Header.MusicCarouselShelfBasicHeaderRenderer(
            moreContentButton = null,
            title = Runs(runs = listOf(Runs.Run(text = title, navigationEndpoint = null))),
            strapline = strapline?.let {
                Runs(runs = listOf(Runs.Run(text = it, navigationEndpoint = null)))
            }
        )
    )

    private fun watchEndpoint(videoId: String) = NavigationEndpoint(
        watchEndpoint = NavigationEndpoint.Endpoint.Watch(videoId = videoId),
        watchPlaylistEndpoint = null,
        browseEndpoint = null,
        searchEndpoint = null
    )

    private fun browseEndpoint(browseId: String, pageType: String) = NavigationEndpoint(
        watchEndpoint = null,
        watchPlaylistEndpoint = null,
        browseEndpoint = NavigationEndpoint.Endpoint.Browse(
            browseId = browseId,
            browseEndpointContextSupportedConfigs = NavigationEndpoint.Endpoint.Browse.BrowseEndpointContextSupportedConfigs(
                browseEndpointContextMusicConfig = NavigationEndpoint.Endpoint.Browse.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig(
                    pageType = pageType
                )
            )
        ),
        searchEndpoint = null
    )

    private fun thumbnail() = ThumbnailRenderer(
        musicThumbnailRenderer = ThumbnailRenderer.MusicThumbnailRenderer(
            thumbnail = ThumbnailRenderer.MusicThumbnailRenderer.Thumbnail(
                thumbnails = listOf(Thumbnail(url = "https://example.com/t.jpg", height = 120, width = 120))
            )
        )
    )
}
