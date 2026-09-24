package it.vfsfitvnm.vimusic.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.compose.persist.persist
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.bodies.SearchBody
import it.vfsfitvnm.innertube.requests.searchPage
import it.vfsfitvnm.innertube.utils.from
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.items.SongItem
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.UiStrings
import it.vfsfitvnm.vimusic.utils.Region
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.forcePlayAtIndex
import it.vfsfitvnm.vimusic.utils.forcePlayFromBeginning
import it.vfsfitvnm.vimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.Locale

data class RegionalSection(
    val id: String,
    val title: String,
    val query: String
)

/**
 * Home sections with what is popular where the listener is. Egypt gets Egyptian
 * trends plus new, most played and classic Arabic music; other Arab countries get
 * their local trends plus the Arabic sections; everyone else gets their own
 * country's trends and hits. YouTube Music ranks the searches with `gl` set to the
 * same region, so results stay local.
 */
fun regionalSections(region: String?, strings: UiStrings, locale: Locale): List<RegionalSection> {
    val code = region?.uppercase() ?: return emptyList()
    val year = Calendar.getInstance().get(Calendar.YEAR)
    val countryName = Region.displayName(code, locale)
    val arabicName = Region.displayName(code, Locale("ar"))
    val englishName = Region.displayName(code, Locale.ENGLISH)

    val arabicSections = listOf(
        RegionalSection("arabic_new", strings.newArabicSongs, "اغاني عربية جديدة $year"),
        RegionalSection("arabic_top", strings.topArabicSongs, "اشهر الاغاني العربية الاكثر استماعا"),
        RegionalSection("arabic_classic", strings.classicArabicSongs, "روائع الطرب العربي الأصيل")
    )

    return when {
        code == "EG" -> listOf(
            RegionalSection("eg_trending", strings.trendingIn(countryName), "ترند مصر اغاني $year"),
            RegionalSection("eg_top", strings.mostPopularIn(countryName), "اغاني مصرية الاكثر استماعا $year")
        ) + arabicSections

        Region.isArab(code) -> listOf(
            RegionalSection("${code}_trending", strings.trendingIn(countryName), "ترند $arabicName اغاني $year"),
            RegionalSection("${code}_top", strings.mostPopularIn(countryName), "اغاني $arabicName الاكثر استماعا $year")
        ) + arabicSections

        else -> listOf(
            RegionalSection("${code}_trending", strings.trendingIn(countryName), "trending songs $englishName $year"),
            RegionalSection("${code}_top", strings.mostPopularIn(countryName), "top hits $englishName $year"),
            RegionalSection("${code}_new", strings.newReleases, "new songs $year $englishName")
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun RegionalSongsSection(
    section: RegionalSection,
    region: String,
    itemWidth: Dp,
    thumbnailSizeDp: Dp,
    thumbnailSizePx: Int,
    contentPadding: PaddingValues,
    titleModifier: Modifier
) {
    val (_, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val strings = it.vfsfitvnm.vimusic.utils.LocalStrings.current

    var songs by persist<List<Innertube.SongItem>?>("home/regional/$region/${section.id}")

    LaunchedEffect(section.query, region) {
        if (songs != null) return@LaunchedEffect
        songs = withTimeoutOrNull(20_000) {
            withContext(Dispatchers.IO) {
                Innertube.searchPage(
                    body = SearchBody(
                        query = section.query,
                        params = Innertube.SearchFilter.Song.value
                    ),
                    fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                )?.getOrNull()?.items
            }
        }?.distinctBy { it.key }?.take(15)
    }

    val items = songs?.takeIf { it.isNotEmpty() } ?: return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicText(
            text = section.title,
            style = typography.m.semiBold,
            modifier = titleModifier.weight(1f)
        )
        SecondaryTextButton(
            text = strings.playAll,
            onClick = {
                binder?.stopRadio()
                binder?.player?.forcePlayFromBeginning(items.map { it.asMediaItem })
            },
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)
        )
    }

    LazyRow(contentPadding = contentPadding) {
        items(items, key = Innertube.SongItem::key) { song ->
            SongItem(
                song = song,
                thumbnailSizePx = thumbnailSizePx,
                thumbnailSizeDp = thumbnailSizeDp,
                modifier = Modifier
                    .width(itemWidth)
                    .combinedClickable(
                        onLongClick = {
                            menuState.display {
                                NonQueuedMediaItemMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = song.asMediaItem
                                )
                            }
                        },
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(
                                items.map { it.asMediaItem },
                                items.indexOf(song).coerceAtLeast(0)
                            )
                        }
                    )
            )
        }
    }
}
