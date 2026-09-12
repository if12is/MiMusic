package it.vfsfitvnm.kugou

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

object LrcLib {
    data class Track(
        val plain: String?,
        val synced: String?,
        val durationSeconds: Long? = null
    ) {
        val hasText: Boolean
            get() = !plain.isNullOrBlank() || !synced.isNullOrBlank()
    }

    @Serializable
    private data class Response(
        val trackName: String? = null,
        val artistName: String? = null,
        val duration: Double? = null,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null
    )

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false

            install(UserAgent) {
                agent = "MiMusic/0.6.7 (https://github.com/if12is/MiMusic)"
            }

            install(ContentNegotiation) {
                json(json)
                json(json, ContentType.Application.Json)
            }

            defaultRequest {
                url("https://lrclib.net")
            }
        }
    }

    suspend fun lyrics(artist: String, title: String, durationSeconds: Long): Result<Track?>? {
        return runCatching {
            val cleanedTitle = title.trim()
            val cleanedArtist = artist.trim()
                .takeUnless { it.equals("null", ignoreCase = true) }
                .orEmpty()
            if (cleanedTitle.isEmpty()) return@runCatching null

            getExact(cleanedArtist, cleanedTitle, durationSeconds)
                ?: searchBest(cleanedArtist, cleanedTitle, durationSeconds)
        }.recoverIfCancelled()
    }

    private suspend fun getExact(artist: String, title: String, durationSeconds: Long): Track? {
        if (artist.isEmpty()) return null
        val response = client.get("/api/get") {
            parameter("artist_name", artist)
            parameter("track_name", title)
            parameter("duration", durationSeconds)
        }
        if (!response.status.isSuccess()) return null
        return response.body<Response>().toTrack()
    }

    private suspend fun searchBest(artist: String, title: String, durationSeconds: Long): Track? {
        val queries = buildList {
            if (artist.isNotEmpty()) add("$artist $title")
            add(title)
        }

        queries.forEach { query ->
            val response = client.get("/api/search") {
                parameter("q", query)
            }
            if (!response.status.isSuccess()) return@forEach

            val tracks = response.body<List<Response>>()
                .mapNotNull { responseItem -> responseItem.toTrack() }
                .filter(Track::hasText)
            if (tracks.isEmpty()) return@forEach

            return tracks.firstOrNull { track ->
                val duration = track.durationSeconds ?: return@firstOrNull false
                abs(duration - durationSeconds) <= 8
            } ?: tracks.first()
        }

        return null
    }

    private fun Response.toTrack(): Track? {
        val plain = plainLyrics?.trim()?.takeIf { it.isNotEmpty() }
        val synced = syncedLyrics?.trim()?.takeIf { it.isNotEmpty() }
        if (plain == null && synced == null) return null
        return Track(
            plain = plain ?: synced?.let(LrcParser::plainText),
            synced = synced,
            durationSeconds = duration?.toLong()
        )
    }
}
