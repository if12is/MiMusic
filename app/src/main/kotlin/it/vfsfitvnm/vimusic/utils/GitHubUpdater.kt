package it.vfsfitvnm.vimusic.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import it.vfsfitvnm.innertube.utils.compareVersionNames
import it.vfsfitvnm.vimusic.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tagName: String,
    val versionName: String,
    val apkUrl: String,
    val htmlUrl: String,
    val notes: String
) {
    val isNewer: Boolean
        get() = compareVersionNames(versionName, BuildConfig.VERSION_NAME) > 0
}

object GitHubUpdater {
    const val owner = "if12is"
    const val repo = "MiMusic"
    const val latestApiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
    const val releasesUrl = "https://github.com/$owner/$repo/releases"

    fun fetchLatestRelease(): GitHubRelease? {
        val connection = (URL(latestApiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MiMusic")
            connectTimeout = 15000
            readTimeout = 15000
        }

        return try {
            if (connection.responseCode !in 200..299) {
                return null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.optString("tag_name").ifBlank { return null }
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl: String? = null
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }

            GitHubRelease(
                tagName = tagName,
                versionName = tagName.removePrefix("v"),
                apkUrl = apkUrl ?: return null,
                htmlUrl = json.optString("html_url", releasesUrl),
                notes = json.optString("body").orEmpty()
            )
        } finally {
            connection.disconnect()
        }
    }

    fun downloadApk(context: Context, apkUrl: String): File {
        val destination = File(context.cacheDir, "updates/MiMusic-latest.apk")
        destination.parentFile?.mkdirs()

        val connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "MiMusic")
            connectTimeout = 30000
            readTimeout = 60000
        }

        try {
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            connection.disconnect()
        }

        return destination
    }

    fun installApk(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun canRequestPackageInstalls(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun installPermissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
}

fun updateInstallPendingIntent(context: Context, apk: File): PendingIntent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apk)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        (if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0) or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
