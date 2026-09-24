package it.vfsfitvnm.vimusic.utils

import android.content.SharedPreferences
import androidx.core.content.edit
import it.vfsfitvnm.innertube.utils.ExtraMediaIds
import it.vfsfitvnm.innertube.utils.ExtraTrack
import org.json.JSONObject
import java.util.Locale

const val jellyfinServerKey = "jellyfinServer"
const val jellyfinUserKey = "jellyfinUser"
const val jellyfinPasswordKey = "jellyfinPassword"
const val jellyfinTokenKey = "jellyfinToken"
const val jellyfinUserIdKey = "jellyfinUserId"

object Jellyfin {
    fun connect(preferences: SharedPreferences): Boolean {
        val server = normalizeServer(preferences.getString(jellyfinServerKey, null)) ?: return false
        val user = preferences.getString(jellyfinUserKey, null)?.trim().orEmpty()
        val password = SecretStore.get(jellyfinPasswordKey)
        if (user.isEmpty()) return false

        val body = JSONObject()
            .put("Username", user)
            .put("Pw", password)
            .toString()
        val json = JSONObject(
            exchangeHttp(
                url = "$server/Users/AuthenticateByName",
                method = "POST",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-Emby-Authorization" to embyHeader()
                ),
                body = body
            )
        )
        val token = json.optString("AccessToken")
        val userId = json.optJSONObject("User")?.optString("Id").orEmpty()
        if (token.isBlank() || userId.isBlank()) return false
        SecretStore.put(jellyfinTokenKey, token)
        preferences.edit { putString(jellyfinUserIdKey, userId) }
        return true
    }

    fun search(preferences: SharedPreferences, query: String, limit: Int = 30): List<ExtraTrack> {
        val session = session(preferences) ?: return emptyList()
        val encoded = java.net.URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
        val url = buildString {
            append(session.server)
            append("/Users/")
            append(session.userId)
            append("/Items?Recursive=true&IncludeItemTypes=Audio,MusicAlbum,AudioBook")
            append("&Limit=")
            append(limit)
            if (query.isNotBlank()) {
                append("&SearchTerm=")
                append(encoded)
            }
            append("&Fields=PrimaryImageAspectRatio,BasicSyncInfo")
        }
        val json = JSONObject(exchangeHttp(url, headers = mapOf("X-Emby-Token" to session.token)))
        val items = json.optJSONArray("Items") ?: return emptyList()
        val tracks = ArrayList<ExtraTrack>(items.length())
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val type = item.optString("Type")
            if (type != "Audio" && type != "AudioBook") continue
            val id = item.optString("Id")
            val name = item.optString("Name")
            if (id.isBlank() || name.isBlank()) continue
            val artist = item.optJSONArray("Artists")?.optString(0)
                ?: item.optString("AlbumArtist").takeIf { it.isNotBlank() }
            val ticks = item.optLong("RunTimeTicks", 0L)
            val imageTag = item.optJSONObject("ImageTags")?.optString("Primary")
            val image = if (!imageTag.isNullOrBlank()) {
                "${session.server}/Items/$id/Images/Primary?tag=$imageTag&maxWidth=300"
            } else {
                null
            }
            tracks += ExtraTrack(
                mediaId = ExtraMediaIds.jellyfin(id),
                title = name,
                artist = artist,
                durationText = formatTicks(ticks),
                thumbnailUrl = image,
                source = "Jellyfin"
            )
        }
        return tracks
    }

    fun streamUrl(preferences: SharedPreferences, itemId: String): String {
        val session = session(preferences) ?: error("Jellyfin is not connected")
        return "${session.server}/Audio/$itemId/universal?UserId=${session.userId}" +
            "&api_key=${session.token}&Container=mp3,aac,m4a,flac,ogg,wav" +
            "&TranscodingContainer=mp3&TranscodingProtocol=http"
    }

    fun isConnected(preferences: SharedPreferences): Boolean = session(preferences) != null

    private data class Session(val server: String, val token: String, val userId: String)

    private fun session(preferences: SharedPreferences): Session? {
        val server = normalizeServer(preferences.getString(jellyfinServerKey, null)) ?: return null
        val token = SecretStore.get(jellyfinTokenKey)
        val userId = preferences.getString(jellyfinUserIdKey, null)?.trim().orEmpty()
        if (token.isEmpty() || userId.isEmpty()) return null
        return Session(server, token, userId)
    }

    private fun normalizeServer(raw: String?): String? {
        val value = raw?.trim()?.trimEnd('/') ?: return null
        if (!value.startsWith("http://") && !value.startsWith("https://")) return null
        if (!CleartextPolicy.allows(value)) return null
        return value
    }

    private fun embyHeader(): String {
        return "MediaBrowser Client=\"MiMusic\", Device=\"Android\", DeviceId=\"mimusic\", Version=\"0.7.1\""
    }

    private fun formatTicks(ticks: Long): String? {
        if (ticks <= 0L) return null
        val total = (ticks / 10_000_000L).toInt()
        val minutes = total / 60
        val secs = total % 60
        return String.format(Locale.US, "%d:%02d", minutes, secs)
    }
}
