package it.vfsfitvnm.vimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.format.Formatter
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
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.service.PlayerMediaBrowserService
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.components.themed.TextFieldDialog
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.Jellyfin
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid12
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.isIgnoringBatteryOptimizations
import it.vfsfitvnm.vimusic.utils.isInvincibilityEnabledKey
import it.vfsfitvnm.vimusic.utils.appLockKey
import it.vfsfitvnm.vimusic.utils.hideFromRecentsKey
import it.vfsfitvnm.vimusic.utils.jellyfinPasswordKey
import it.vfsfitvnm.vimusic.utils.jellyfinServerKey
import it.vfsfitvnm.vimusic.utils.jellyfinUserKey
import it.vfsfitvnm.vimusic.utils.listenBrainzEnabledKey
import it.vfsfitvnm.vimusic.utils.listenBrainzTokenKey
import it.vfsfitvnm.vimusic.utils.mobileDataBytes
import it.vfsfitvnm.vimusic.utils.pauseSearchHistoryKey
import it.vfsfitvnm.vimusic.utils.podcastFeedsKey
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.rememberSecret
import it.vfsfitvnm.vimusic.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun OtherSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    val strings = LocalStrings.current

    var isAndroidAutoEnabled by remember {
        val component = ComponentName(context, PlayerMediaBrowserService::class.java)
        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        mutableStateOf(
            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
            policy = object : SnapshotMutationPolicy<Boolean> {
                override fun equivalent(a: Boolean, b: Boolean): Boolean {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        if (b) enabledFlag else disabledFlag,
                        PackageManager.DONT_KILL_APP
                    )
                    return a == b
                }
            }
        )
    }

    var isInvincibilityEnabled by rememberPreference(isInvincibilityEnabledKey, false)

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations)
    }

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations
        }

    var pauseSearchHistory by rememberPreference(pauseSearchHistoryKey, false)
    var appLock by rememberPreference(appLockKey, false)
    var hideFromRecents by rememberPreference(hideFromRecentsKey, false)
    var listenBrainzEnabled by rememberPreference(listenBrainzEnabledKey, false)
    var listenBrainzToken by rememberSecret(listenBrainzTokenKey, "")
    var podcastFeeds by rememberPreference(podcastFeedsKey, "")
    var jellyfinServer by rememberPreference(jellyfinServerKey, "")
    var jellyfinUser by rememberPreference(jellyfinUserKey, "")
    var jellyfinPassword by rememberSecret(jellyfinPasswordKey, "")
    var editingToken by remember { mutableStateOf(false) }
    var editingFeeds by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf(false) }
    var editingPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

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
        Header(title = strings.other)

        SettingsEntryGroupText(title = strings.extraSources)

        SwitchSettingEntry(
            title = strings.listenBrainz,
            text = strings.listenBrainzDescription,
            isChecked = listenBrainzEnabled,
            onCheckedChange = { listenBrainzEnabled = it }
        )

        SettingsEntry(
            title = strings.listenBrainzToken,
            text = if (listenBrainzToken.isBlank()) strings.listenBrainzTokenHint else "••••••••",
            onClick = { editingToken = true }
        )

        SettingsEntry(
            title = strings.podcastFeeds,
            text = if (podcastFeeds.isBlank()) strings.podcastFeedsHint else podcastFeeds.lineSequence().count { it.isNotBlank() }.toString(),
            onClick = { editingFeeds = true }
        )

        SettingsEntry(
            title = strings.jellyfinServer,
            text = jellyfinServer.ifBlank { strings.jellyfin },
            onClick = { editingServer = true }
        )

        SettingsEntry(
            title = strings.jellyfinUser,
            text = jellyfinUser.ifBlank { strings.jellyfinUser },
            onClick = { editingUser = true }
        )

        SettingsEntry(
            title = strings.jellyfinPassword,
            text = if (jellyfinPassword.isBlank()) strings.jellyfinPassword else "••••••••",
            onClick = { editingPassword = true }
        )

        SettingsEntry(
            title = strings.jellyfinConnect,
            text = if (Jellyfin.isConnected(context.preferences)) strings.jellyfinConnected else strings.jellyfin,
            onClick = {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching { Jellyfin.connect(context.preferences) }.getOrDefault(false)
                    }
                    context.toast(if (ok) strings.jellyfinConnected else strings.jellyfinConnectFailed)
                }
            }
        )

        if (editingToken) {
            TextFieldDialog(
                hintText = strings.listenBrainzTokenHint,
                initialTextInput = listenBrainzToken,
                onDismiss = { editingToken = false },
                onDone = {
                    listenBrainzToken = it.trim()
                    editingToken = false
                },
                isTextInputValid = { true }
            )
        }

        if (editingFeeds) {
            TextFieldDialog(
                hintText = strings.podcastFeedsHint,
                initialTextInput = podcastFeeds,
                singleLine = false,
                maxLines = 8,
                onDismiss = { editingFeeds = false },
                onDone = {
                    podcastFeeds = it
                    editingFeeds = false
                },
                isTextInputValid = { true }
            )
        }

        if (editingServer) {
            TextFieldDialog(
                hintText = "http://192.168.1.10:8096",
                initialTextInput = jellyfinServer,
                onDismiss = { editingServer = false },
                onDone = {
                    jellyfinServer = it.trim()
                    editingServer = false
                },
                isTextInputValid = { true }
            )
        }

        if (editingUser) {
            TextFieldDialog(
                hintText = strings.jellyfinUser,
                initialTextInput = jellyfinUser,
                onDismiss = { editingUser = false },
                onDone = {
                    jellyfinUser = it.trim()
                    editingUser = false
                },
                isTextInputValid = { true }
            )
        }

        if (editingPassword) {
            TextFieldDialog(
                hintText = strings.jellyfinPassword,
                initialTextInput = jellyfinPassword,
                onDismiss = { editingPassword = false },
                onDone = {
                    jellyfinPassword = it
                    editingPassword = false
                },
                isTextInputValid = { true }
            )
        }

        SettingsGroupSpacer()

        SwitchSettingEntry(
            title = strings.appLock,
            text = strings.appLockDescription,
            isChecked = appLock,
            onCheckedChange = { appLock = it }
        )

        SwitchSettingEntry(
            title = strings.hideRecents,
            text = strings.hideRecentsDescription,
            isChecked = hideFromRecents,
            onCheckedChange = { hideFromRecents = it }
        )

        val dataBytes = remember { context.mobileDataBytes() }
        SettingsDescription(
            text = "${strings.dataUsage}: ${Formatter.formatShortFileSize(context, dataBytes)}"
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.androidAuto)

        SettingsDescription(text = strings.androidAutoHint)

        SwitchSettingEntry(
            title = strings.androidAutoTitle,
            text = strings.androidAutoDescription,
            isChecked = isAndroidAutoEnabled,
            onCheckedChange = { isAndroidAutoEnabled = it }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.searchHistory)

        SwitchSettingEntry(
            title = strings.pauseSearchHistory,
            text = strings.pauseSearchHistoryDescription,
            isChecked = pauseSearchHistory,
            onCheckedChange = { pauseSearchHistory = it }
        )

        SettingsEntry(
            title = strings.clearSearchHistory,
            text = if (queriesCount > 0) {
                strings.deleteSearchQueries(queriesCount)
            } else {
                strings.historyEmpty
            },
            isEnabled = queriesCount > 0,
            onClick = { query(Database::clearQueries) }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.serviceLifetime)

        ImportantSettingsDescription(text = strings.batteryOptimizationWarning)

        if (isAtLeastAndroid12) {
            SettingsDescription(text = strings.batteryOptimizationAndroid12)
        }

        SettingsEntry(
            title = strings.ignoreBatteryOptimizations,
            isEnabled = !isIgnoringBatteryOptimizations,
            text = if (isIgnoringBatteryOptimizations) {
                strings.alreadyUnrestricted
            } else {
                strings.disableBackgroundRestrictions
            },
            onClick = {
                if (!isAtLeastAndroid6) return@SettingsEntry

                try {
                    activityResultLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    try {
                        activityResultLauncher.launch(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    } catch (e: ActivityNotFoundException) {
                        context.toast(strings.batterySettingsMissing)
                    }
                }
            }
        )

        SwitchSettingEntry(
            title = strings.invincibleService,
            text = strings.invincibleServiceDescription,
            isChecked = isInvincibilityEnabled,
            onCheckedChange = { isInvincibilityEnabled = it }
        )
    }
}
