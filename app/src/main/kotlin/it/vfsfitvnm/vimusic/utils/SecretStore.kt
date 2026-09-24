package it.vfsfitvnm.vimusic.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Jellyfin passwords and ListenBrainz tokens live outside the normal preferences
 * file, encrypted on Android 6+, and are excluded from backups.
 */
object SecretStore {
    const val FILE = "mimusic_secrets"

    private val secretKeys = setOf(
        listenBrainzTokenKey,
        jellyfinPasswordKey,
        jellyfinTokenKey
    )

    @Volatile
    private var appContext: Context? = null
    private val lock = Any()
    private var cached: SharedPreferences? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(key: String): String {
        val context = appContext ?: return ""
        val secure = prefs(context).getString(key, null)?.trim().orEmpty()
        if (secure.isNotEmpty()) return secure
        if (key !in secretKeys) return ""
        val legacy = context.preferences.getString(key, null)?.trim().orEmpty()
        if (legacy.isNotEmpty()) {
            put(key, legacy)
        }
        return legacy
    }

    fun put(key: String, value: String) {
        val context = appContext ?: return
        prefs(context).edit {
            if (value.isBlank()) remove(key) else putString(key, value)
        }
        context.preferences.edit { remove(key) }
    }

    private fun prefs(context: Context): SharedPreferences {
        cached?.let { return it }
        return synchronized(lock) {
            cached ?: open(context).also { cached = it }
        }
    }

    private fun open(context: Context): SharedPreferences {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }
}
