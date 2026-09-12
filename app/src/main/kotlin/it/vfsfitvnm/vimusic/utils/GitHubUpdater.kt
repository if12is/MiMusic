package it.vfsfitvnm.vimusic.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import it.vfsfitvnm.innertube.utils.compareVersionNames
import it.vfsfitvnm.vimusic.BuildConfig
import it.vfsfitvnm.vimusic.service.UpdateInstallReceiver
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

sealed class UpdateInstallResult {
    data object Started : UpdateInstallResult()
    data object SignatureMismatch : UpdateInstallResult()
    data object PackageMismatch : UpdateInstallResult()
    data class Failed(val message: String?) : UpdateInstallResult()
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

    fun installApk(context: Context, apk: File): UpdateInstallResult {
        val archiveInfo = context.packageManager.getPackageArchiveInfoCompat(apk) ?: return UpdateInstallResult.Failed("apk")
        val apkPackage = archiveInfo.packageName
        if (apkPackage != context.packageName) {
            return UpdateInstallResult.PackageMismatch
        }
        if (!signaturesCompatible(context, apk)) {
            return UpdateInstallResult.SignatureMismatch
        }

        return runCatching {
            installWithPackageInstaller(context, apk)
            UpdateInstallResult.Started
        }.getOrElse { error ->
            runCatching {
                startLegacyInstaller(context, apk)
                UpdateInstallResult.Started
            }.getOrElse {
                UpdateInstallResult.Failed(error.message)
            }
        }
    }

    private fun installWithPackageInstaller(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("MiMusic.apk", 0, apk.length()).use { output ->
                apk.inputStream().use { input -> input.copyTo(output) }
                session.fsync(output)
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            )
            session.commit(pendingIntent.intentSender)
        }
    }

    private fun startLegacyInstaller(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            putExtra("android.intent.extra.INSTALLER_PACKAGE_NAME", context.packageName)
        }
        context.packageManager.queryIntentActivities(intent, 0).forEach { resolve ->
            context.grantUriPermission(
                resolve.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        context.startActivity(intent)
    }

    private fun signaturesCompatible(context: Context, apk: File): Boolean {
        val incoming = context.packageManager.getPackageArchiveInfoCompat(apk)?.signingCerts().orEmpty()
        val installed = runCatching {
            context.packageManager.getPackageInfoCompat(context.packageName)?.signingCerts().orEmpty()
        }.getOrDefault(emptyList())
        if (incoming.isEmpty() || installed.isEmpty()) return true
        return incoming.any { signature -> installed.contains(signature) }
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

private fun PackageManager.getPackageArchiveInfoCompat(apk: File) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageArchiveInfo(
            apk.absolutePath,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        @Suppress("DEPRECATION")
        getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
    } else {
        @Suppress("DEPRECATION")
        getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNATURES)
    }?.also { info ->
        info.applicationInfo?.sourceDir = apk.absolutePath
        info.applicationInfo?.publicSourceDir = apk.absolutePath
    }

private fun PackageManager.getPackageInfoCompat(packageName: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
    }

private fun android.content.pm.PackageInfo.signingCerts(): List<String> {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        signingInfo?.apkContentsSigners
    } else {
        @Suppress("DEPRECATION")
        signatures
    }
    return signatures.orEmpty().map { android.util.Base64.encodeToString(it.toByteArray(), android.util.Base64.NO_WRAP) }
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
