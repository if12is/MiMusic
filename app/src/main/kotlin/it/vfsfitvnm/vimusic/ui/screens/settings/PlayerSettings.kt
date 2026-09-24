package it.vfsfitvnm.vimusic.ui.screens.settings

import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.videoModeKey
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import it.vfsfitvnm.vimusic.LocalPlayerAwareWindowInsets
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.ui.components.themed.Header
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.persistentQueueKey
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.resumePlaybackWhenDeviceConnectedKey
import it.vfsfitvnm.vimusic.utils.skipSilenceKey
import it.vfsfitvnm.vimusic.utils.toast
import it.vfsfitvnm.vimusic.enums.AudioQuality
import it.vfsfitvnm.vimusic.utils.audioQualityKey
import it.vfsfitvnm.vimusic.utils.carModeKey
import it.vfsfitvnm.vimusic.utils.crossfadeEnabledKey
import it.vfsfitvnm.vimusic.utils.offlineModeKey
import it.vfsfitvnm.vimusic.utils.smartShuffleKey
import it.vfsfitvnm.vimusic.utils.keepScreenOnKey
import it.vfsfitvnm.vimusic.utils.volumeNormalizationKey
import it.vfsfitvnm.vimusic.utils.wifiOnlyDownloadKey

@ExperimentalAnimationApi
@Composable
fun PlayerSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val strings = LocalStrings.current

    var persistentQueue by rememberPreference(persistentQueueKey, false)
    var resumePlaybackWhenDeviceConnected by rememberPreference(
        resumePlaybackWhenDeviceConnectedKey,
        false
    )
    var skipSilence by rememberPreference(skipSilenceKey, false)
    var volumeNormalization by rememberPreference(volumeNormalizationKey, false)
    var keepScreenOn by rememberPreference(keepScreenOnKey, false)
    var audioQuality by rememberPreference(audioQualityKey, AudioQuality.Auto)
    var crossfadeEnabled by rememberPreference(crossfadeEnabledKey, false)
    var wifiOnlyDownload by rememberPreference(wifiOnlyDownloadKey, false)
    var offlineMode by rememberPreference(offlineModeKey, false)
    var carMode by rememberPreference(carModeKey, false)
    var smartShuffle by rememberPreference(smartShuffleKey, true)

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

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
        Header(title = strings.playerAndAudio)

        SettingsEntryGroupText(title = strings.playerGroup)

        SwitchSettingEntry(
            title = strings.persistentQueue,
            text = strings.persistentQueueDescription,
            isChecked = persistentQueue,
            onCheckedChange = {
                persistentQueue = it
            }
        )

        if (isAtLeastAndroid6) {
            SwitchSettingEntry(
                title = strings.resumePlayback,
                text = strings.resumePlaybackDescription,
                isChecked = resumePlaybackWhenDeviceConnected,
                onCheckedChange = {
                    resumePlaybackWhenDeviceConnected = it
                }
            )
        }

        SwitchSettingEntry(
            title = strings.keepScreenOn,
            text = strings.keepScreenOnDescription,
            isChecked = keepScreenOn,
            onCheckedChange = { keepScreenOn = it }
        )

        SwitchSettingEntry(
            title = strings.carMode,
            text = strings.carModeDescription,
            isChecked = carMode,
            onCheckedChange = { carMode = it }
        )

        SwitchSettingEntry(
            title = strings.smartShuffle,
            text = strings.smartShuffleDescription,
            isChecked = smartShuffle,
            onCheckedChange = { smartShuffle = it }
        )

        SwitchSettingEntry(
            title = strings.offlineMode,
            text = strings.offlineModeDescription,
            isChecked = offlineMode,
            onCheckedChange = { offlineMode = it }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = strings.audioGroup)

        SwitchSettingEntry(
            title = strings.skipSilence,
            text = strings.skipSilenceDescription,
            isChecked = skipSilence,
            onCheckedChange = {
                skipSilence = it
            }
        )

        SwitchSettingEntry(
            title = strings.loudnessNormalization,
            text = strings.loudnessNormalizationDescription,
            isChecked = volumeNormalization,
            onCheckedChange = {
                volumeNormalization = it
            }
        )

        EnumValueSelectorSettingsEntry(
            title = strings.audioQuality,
            selectedValue = audioQuality,
            onValueSelected = { audioQuality = it },
            valueText = strings::audioQualityName
        )

        SwitchSettingEntry(
            title = strings.crossfade,
            text = strings.crossfadeDescription,
            isChecked = crossfadeEnabled,
            onCheckedChange = { crossfadeEnabled = it }
        )

        SwitchSettingEntry(
            title = strings.wifiOnlyDownload,
            text = strings.wifiOnlyDownloadDescription,
            isChecked = wifiOnlyDownload,
            onCheckedChange = { wifiOnlyDownload = it }
        )

        var chargingOnlyDownload by rememberPreference(it.vfsfitvnm.vimusic.utils.chargingOnlyDownloadKey, false)
        SwitchSettingEntry(
            title = strings.chargingOnlyDownload,
            text = strings.chargingOnlyDownloadDescription,
            isChecked = chargingOnlyDownload,
            onCheckedChange = { chargingOnlyDownload = it }
        )

        var pipOnLeave by rememberPreference(it.vfsfitvnm.vimusic.utils.pipOnLeaveKey, false)
        SwitchSettingEntry(
            title = strings.pictureInPicture,
            text = strings.pictureInPictureDescription,
            isChecked = pipOnLeave,
            onCheckedChange = { pipOnLeave = it }
        )

        val videoMode by rememberPreference(videoModeKey, false)
        SwitchSettingEntry(
            title = strings.videoModeSetting,
            text = strings.videoModeSettingDescription,
            isChecked = videoMode,
            onCheckedChange = { enabled ->
                binder?.setVideoMode(enabled)
                    ?: context.preferences.edit().putBoolean(videoModeKey, enabled).apply()
            }
        )

        var videoLyrics by rememberPreference(it.vfsfitvnm.vimusic.utils.videoLyricsKey, true)
        SwitchSettingEntry(
            title = strings.videoLyrics,
            text = strings.videoLyricsDescription,
            isChecked = videoLyrics,
            onCheckedChange = { videoLyrics = it }
        )

        SettingsEntry(
            title = strings.equalizer,
            text = strings.equalizerDescription,
            onClick = {
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder?.player?.audioSessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }

                try {
                    activityResultLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    context.toast(strings.equalizerMissing)
                }
            }
        )
    }
}
