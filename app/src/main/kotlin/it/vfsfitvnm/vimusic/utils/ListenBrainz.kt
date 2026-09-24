package it.vfsfitvnm.vimusic.utils

import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

const val listenBrainzEnabledKey = "listenBrainzEnabled"
const val listenBrainzTokenKey = "listenBrainzToken"

object ListenBrainz {
    fun submit(preferences: SharedPreferences, mediaItem: MediaItem, playTimeMs: Long) {
        if (!preferences.getBoolean(listenBrainzEnabledKey, false)) return
        val token = SecretStore.get(listenBrainzTokenKey)
        if (token.isEmpty() || playTimeMs < 30_000L) return

        val title = mediaItem.mediaMetadata.title?.toString()?.trim().orEmpty()
        val artist = mediaItem.mediaMetadata.artist?.toString()?.trim().orEmpty()
        if (title.isEmpty()) return

        val payload = JSONObject()
            .put("listen_type", "single")
            .put(
                "payload",
                JSONArray().put(
                    JSONObject()
                        .put("listened_at", System.currentTimeMillis() / 1000)
                        .put(
                            "track_metadata",
                            JSONObject()
                                .put("track_name", title)
                                .put("artist_name", artist.ifEmpty { "Unknown" })
                                .put(
                                    "additional_info",
                                    JSONObject()
                                        .put("listening_from", "MiMusic")
                                        .put("duration_ms", playTimeMs)
                                )
                        )
                )
            )

        val connection = (URL("https://api.listenbrainz.org/1/submit-listens").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 12_000
            doOutput = true
            setRequestProperty("Authorization", "Token $token")
            setRequestProperty("Content-Type", "application/json")
        }
        connection.outputStream.use { stream ->
            stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        }
        val code = connection.responseCode
        connection.disconnect()
        if (code !in 200..299) {
            error("ListenBrainz HTTP $code")
        }
    }
}
