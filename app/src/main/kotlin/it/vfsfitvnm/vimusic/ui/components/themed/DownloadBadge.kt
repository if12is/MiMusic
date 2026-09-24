package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.service.DownloadStatus
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.semiBold
@Composable
fun BoxScope.DownloadBadge(mediaId: String) {
    val binder = LocalPlayerServiceBinder.current ?: return
    val (colorPalette, typography) = LocalAppearance.current
    val status by binder.downloadStatusFlow(mediaId).collectAsState(initial = binder.downloadStatus(mediaId))
    val percent by binder.downloadPercentFlow(mediaId).collectAsState(initial = binder.downloadPercent(mediaId))

    if (status == DownloadStatus.Downloading) {
        BasicText(
            text = "${percent ?: 0}%",
            style = typography.xxs.semiBold.copy(color = colorPalette.onAccent),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .background(colorPalette.accent, CircleShape)
                .padding(horizontal = 3.dp, vertical = 1.dp)
        )
        return
    }

    if (status != DownloadStatus.Completed) return

    Image(
        painter = painterResource(R.drawable.checkmark),
        contentDescription = null,
        colorFilter = ColorFilter.tint(colorPalette.accent),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(2.dp)
            .size(14.dp)
            .background(colorPalette.background1, CircleShape)
            .padding(2.dp)
    )
}
