package it.vfsfitvnm.innertube.models.bodies

import it.vfsfitvnm.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context = Context.DefaultAndroidVR,
    val videoId: String,
    val playlistId: String? = null,
    val contentCheckOk: Boolean = true,
    val racyCheckOk: Boolean = true,
    val playbackContext: PlaybackContext? = null
) {
    @Serializable
    data class PlaybackContext(
        val contentPlaybackContext: ContentPlaybackContext? = null
    )

    @Serializable
    data class ContentPlaybackContext(
        val signatureTimestamp: Int? = null,
        val html5Preference: String = "HTML5_PREF_WANTS"
    )
}
