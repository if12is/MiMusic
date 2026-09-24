package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.artworkRequestSize
import it.vfsfitvnm.vimusic.utils.lowPowerModeKey
import it.vfsfitvnm.vimusic.utils.rememberPreference

@Composable
fun Artwork(
    data: Any?,
    sizePx: Int,
    modifier: Modifier = Modifier,
    shape: Shape = LocalAppearance.current.thumbnailShape,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val lowPower by rememberPreference(lowPowerModeKey, false)
    val requestSize = artworkRequestSize(sizePx, lowPower)
    AsyncImage(
        model = remember(data, requestSize) {
            ImageRequest.Builder(context)
                .data(data)
                .size(requestSize)
                .build()
        },
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier.clip(shape)
    )
}
