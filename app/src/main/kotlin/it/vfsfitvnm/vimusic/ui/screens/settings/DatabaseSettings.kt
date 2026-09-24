package it.vfsfitvnm.vimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.internal
import it.vfsfitvnm.vimusic.path
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.service.LibraryBackupWorker
import it.vfsfitvnm.vimusic.service.PlayerService
import it.vfsfitvnm.vimusic.service.newestBackupUri
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.backupTreeUriKey
import it.vfsfitvnm.vimusic.utils.intent
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.distinctUntilChanged

@ExperimentalAnimationApi
@Composable
fun DatabaseSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var backupTree by rememberPreference(backupTreeUriKey, "")

    val eventsCount by remember {
        Database.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.sqlite3")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                Database.checkpoint()

                context.applicationContext.contentResolver.openOutputStream(uri)
                    ?.use { outputStream ->
                        FileInputStream(Database.internal.path).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
            }
        }

    val folderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: SecurityException) {
                context.toast(e.message ?: strings.documentsOpenMissing)
                return@rememberLauncherForActivityResult
            }
            backupTree = uri.toString()
            LibraryBackupWorker.schedule(context)
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                Database.checkpoint()
                Database.internal.close()

                context.applicationContext.contentResolver.openInputStream(uri)
                    ?.use { inputStream ->
                        FileOutputStream(Database.internal.path).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                context.stopService(context.intent<PlayerService>())
                exitProcess(0)
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
        Header(title = strings.database)

        SettingsDescription(text = strings.packageMoveDescription)

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.cleanup)

        SettingsEntry(
            title = strings.resetQuickPicks,
            text = if (eventsCount > 0) {
                strings.deletePlaybackEvents(eventsCount)
            } else {
                strings.quickPicksCleared
            },
            isEnabled = eventsCount > 0,
            onClick = { query(Database::clearEvents) }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.backup)

        SettingsDescription(text = strings.backupPreferencesNote)

        SettingsEntry(
            title = strings.backupTitle,
            text = strings.backupDescription,
            onClick = {
                @SuppressLint("SimpleDateFormat")
                val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")

                try {
                    backupLauncher.launch("vimusic_${dateFormat.format(Date())}.db")
                } catch (e: ActivityNotFoundException) {
                    context.toast(strings.documentsCreateMissing)
                }
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.restore)

        ImportantSettingsDescription(text = strings.restoreOverwriteWarning(context.applicationInfo.nonLocalizedLabel.toString()))

        SettingsEntry(
            title = strings.restoreTitle,
            text = strings.restoreDescription,
            onClick = {
                try {
                    restoreLauncher.launch(
                        arrayOf(
                            "application/vnd.sqlite3",
                            "application/x-sqlite3",
                            "application/octet-stream"
                        )
                    )
                } catch (e: ActivityNotFoundException) {
                    context.toast(strings.documentsOpenMissing)
                }
            }
        )

        SettingsGroupSpacer()

        SettingsEntry(
            title = strings.scheduledBackup,
            text = if (backupTree.isBlank()) strings.scheduledBackupPick else strings.scheduledBackupOn,
            onClick = {
                try {
                    folderLauncher.launch(null)
                } catch (e: ActivityNotFoundException) {
                    context.toast(strings.documentsOpenMissing)
                }
            }
        )

        if (backupTree.isNotBlank()) {
            SettingsEntry(
                title = strings.importLatestBackup,
                text = strings.importLatestBackupDescription,
                onClick = {
                    scope.launch {
                        val latest = withContext(Dispatchers.IO) {
                            newestBackupUri(context.applicationContext, backupTree.toUri())
                        }
                        if (latest == null) {
                            context.toast(strings.noScheduledBackup)
                            return@launch
                        }
                        query {
                            Database.checkpoint()
                            Database.internal.close()
                            context.applicationContext.contentResolver.openInputStream(latest)?.use { inputStream ->
                                FileOutputStream(Database.internal.path).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            context.stopService(context.intent<PlayerService>())
                            exitProcess(0)
                        }
                    }
                }
            )

            SettingsEntry(
                title = strings.stopScheduledBackup,
                text = strings.scheduledBackupOn,
                onClick = {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    try {
                        context.contentResolver.releasePersistableUriPermission(backupTree.toUri(), flags)
                    } catch (e: SecurityException) {
                        context.toast(e.message ?: strings.documentsOpenMissing)
                    }
                    LibraryBackupWorker.cancel(context)
                    backupTree = ""
                }
            )
        }
    }
}
