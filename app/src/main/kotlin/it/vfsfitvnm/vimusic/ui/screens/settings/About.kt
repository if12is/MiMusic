package it.vfsfitvnm.vimusic.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import it.vfsfitvnm.vimusic.BuildConfig
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.ui.components.themed.ConfirmationDialog
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import it.vfsfitvnm.vimusic.utils.GitHubRelease
import it.vfsfitvnm.vimusic.utils.GitHubUpdater
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.PlaybackLogStore
import it.vfsfitvnm.vimusic.utils.UpdateInstallResult
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.vimusic.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalAnimationApi
@Composable
fun About() {
    val (colorPalette, typography) = LocalAppearance.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var showInstallDialog by remember { mutableStateOf(false) }

    fun downloadAndInstall(release: GitHubRelease) {
        if (isDownloading) return

        if (!GitHubUpdater.canRequestPackageInstalls(context)) {
            context.startActivity(
                GitHubUpdater.installPermissionIntent(context)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }

        isDownloading = true
        statusText = strings.downloadingUpdate
        coroutineScope.launch(Dispatchers.IO) {
            val apk = runCatching {
                GitHubUpdater.downloadApk(context, release.apkUrl)
            }.getOrNull()

            withContext(Dispatchers.Main) {
                isDownloading = false
                if (apk == null) {
                    statusText = strings.updateCheckFailed
                } else {
                    statusText = when (val result = GitHubUpdater.installApk(context, apk)) {
                        UpdateInstallResult.Started -> null
                        UpdateInstallResult.SignatureMismatch -> strings.updateSignatureMismatch
                        UpdateInstallResult.PackageMismatch -> strings.updatePackageMismatch
                        is UpdateInstallResult.Failed -> result.message
                            ?: strings.updateInstallFailed
                    }
                    if (statusText != null) {
                        context.toast(statusText!!)
                    }
                }
            }
        }
    }

    if (showInstallDialog) {
        latestRelease?.let { release ->
            ConfirmationDialog(
                text = strings.updateAvailableText(release.versionName),
                confirmText = strings.installUpdate,
                onDismiss = { showInstallDialog = false },
                onConfirm = {
                    showInstallDialog = false
                    downloadAndInstall(release)
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        Header(title = strings.about) {
            BasicText(
                text = "v${BuildConfig.VERSION_NAME}",
                style = typography.s.secondary
            )
        }

        SettingsEntryGroupText(title = strings.updates)

        SettingsEntry(
            title = strings.checkForUpdates,
            text = when {
                isDownloading -> strings.downloadingUpdate
                isChecking -> strings.checkForUpdates
                statusText != null -> statusText!!
                else -> strings.checkForUpdatesDescription
            },
            isEnabled = !isChecking && !isDownloading,
            onClick = {
                isChecking = true
                statusText = null
                coroutineScope.launch(Dispatchers.IO) {
                    val release = runCatching { GitHubUpdater.fetchLatestRelease() }.getOrNull()
                    withContext(Dispatchers.Main) {
                        isChecking = false
                        when {
                            release == null -> statusText = strings.updateCheckFailed
                            release.isNewer -> {
                                latestRelease = release
                                showInstallDialog = true
                            }
                            else -> {
                                statusText = strings.upToDate
                                context.toast(strings.upToDate)
                            }
                        }
                    }
                }
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.social)

        SettingsEntry(
            title = strings.github,
            text = strings.viewSource,
            onClick = {
                uriHandler.openUri(GitHubUpdater.releasesUrl.removeSuffix("/releases"))
            }
        )

        SettingsGroupSpacer()

        SettingsEntry(
            title = strings.playbackLog,
            text = strings.playbackLogDescription,
            onClick = {
                val log = PlaybackLogStore.snapshot()
                if (log.isBlank()) {
                    context.toast(strings.playbackLogEmpty)
                } else {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("MiMusic playback log", log))
                    context.toast(strings.playbackLogCopied)
                    runCatching {
                        context.startActivity(
                            PlaybackLogStore.shareIntent().addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        )

        SettingsEntry(
            title = strings.crashLog,
            text = strings.crashLogDescription,
            onClick = {
                if (!PlaybackLogStore.hasCrashReport()) {
                    context.toast(strings.crashLogEmpty)
                    return@SettingsEntry
                }
                val email = PlaybackLogStore.crashEmailIntent()
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(Intent.createChooser(email, strings.crashLog))
                } catch (e: ActivityNotFoundException) {
                    PlaybackLogStore.append("crash email ${e.message}")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("MiMusic crash", email.getStringExtra(Intent.EXTRA_TEXT)))
                    context.toast(strings.crashEmailMissing)
                    runCatching {
                        context.startActivity(
                            PlaybackLogStore.shareIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        )

        SettingsEntryGroupText(title = strings.troubleshooting)

        SettingsEntry(
            title = strings.reportIssue,
            text = strings.redirectedToGithub,
            onClick = {
                uriHandler.openUri("https://github.com/${GitHubUpdater.owner}/${GitHubUpdater.repo}/issues/new")
            }
        )

        SettingsEntry(
            title = strings.requestFeature,
            text = strings.redirectedToGithub,
            onClick = {
                uriHandler.openUri("https://github.com/${GitHubUpdater.owner}/${GitHubUpdater.repo}/issues/new")
            }
        )
    }
}
