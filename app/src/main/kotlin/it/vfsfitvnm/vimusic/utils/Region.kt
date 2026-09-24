package it.vfsfitvnm.vimusic.utils

import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.content.edit
import androidx.core.content.getSystemService
import it.vfsfitvnm.innertube.requests.ArabRegions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

const val detectedRegionKey = "detectedRegion"
const val detectedRegionAtKey = "detectedRegionAt"
const val contentRegionKey = "contentRegion"

/**
 * Works out which country the listener is in, so the home screen and YouTube Music
 * requests (`gl`) return what is popular there instead of a fixed region.
 *
 * Order: public IP geolocation → mobile network / SIM country → device locale.
 * The result is cached and refreshed at most every [RefreshIntervalMs].
 */
object Region {
    private const val RefreshIntervalMs = 12 * 60 * 60 * 1000L

    val ArabCountries = ArabRegions

    private val state = MutableStateFlow<String?>(null)

    /** Emits the region in use; screens reload their regional content when it changes. */
    val flow: StateFlow<String?> = state.asStateFlow()

    /** The last known region, or null before the first detection. */
    val current: String?
        get() = state.value

    fun isArab(region: String?) = region?.uppercase() in ArabCountries

    fun load(context: Context) {
        val manual = selectedCode(context.preferences)
        state.value = manual
            ?: context.preferences.getString(detectedRegionKey, null)
            ?: offlineGuess(context)
    }

    fun useManual(context: Context, code: String) {
        val valid = code.validCode() ?: return
        context.preferences.edit { putString(contentRegionKey, valid) }
        state.value = valid
    }

    fun useAutomatic(context: Context) {
        context.preferences.edit { putString(contentRegionKey, "auto") }
    }

    /** Refreshes the region when the cached value is stale. Returns the region in use. */
    suspend fun refresh(context: Context, force: Boolean = false): String? {
        val preferences = context.preferences
        selectedCode(preferences)?.let { manual ->
            state.value = manual
            return manual
        }
        val last = preferences.getLong(detectedRegionAtKey, 0L)
        if (!force && current != null && System.currentTimeMillis() - last < RefreshIntervalMs) {
            return current
        }

        val detected = withContext(Dispatchers.IO) { fromIp() } ?: offlineGuess(context)
        if (detected != null) {
            preferences.edit {
                putString(detectedRegionKey, detected)
                putLong(detectedRegionAtKey, System.currentTimeMillis())
            }
            state.value = detected
        }
        return current
    }

    fun displayName(region: String, locale: Locale = Locale.getDefault()): String =
        Locale("", region).getDisplayCountry(locale).ifBlank { region }

    val selectableRegions: List<String> =
        (ArabCountries + setOf("US", "GB", "FR", "DE", "TR")).sorted()

    private fun selectedCode(preferences: android.content.SharedPreferences): String? {
        val raw = preferences.getString(contentRegionKey, "auto") ?: "auto"
        if (raw.equals("auto", ignoreCase = true)) return null
        return raw.validCode()
    }

    private fun offlineGuess(context: Context): String? {
        val telephony = context.getSystemService<TelephonyManager>()
        return listOfNotNull(
            runCatching { telephony?.networkCountryIso }.getOrNull(),
            runCatching { telephony?.simCountryIso }.getOrNull()
        ).map { it.trim().uppercase() }.firstOrNull { it.length == 2 }
    }

    private fun fromIp(): String? =
        runCatching {
            // "loc=EG" line of Cloudflare's plain text trace.
            httpGet("https://www.cloudflare.com/cdn-cgi/trace")
                ?.lineSequence()
                ?.firstOrNull { it.startsWith("loc=") }
                ?.removePrefix("loc=")
        }.getOrNull().validCode()
            ?: runCatching { httpGet("https://ipapi.co/country/") }.getOrNull().validCode()

    private fun String?.validCode(): String? =
        this?.trim()?.uppercase()?.takeIf { it.length == 2 && it.all(Char::isLetter) && it != "XX" }

    private fun httpGet(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "MiMusic")
            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }
}
