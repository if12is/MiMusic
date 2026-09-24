@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package it.vfsfitvnm.vimusic.service

import android.os.Binder as AndroidBinder
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Handler
import android.text.format.DateUtils
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.PlaceholderDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.MediaSession as Media3Session
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.audio.SonicAudioProcessor
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.datasource.TransferListener
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mp4.Mp4Extractor
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.NavigationEndpoint
import it.vfsfitvnm.innertube.models.bodies.PlayerBody
import it.vfsfitvnm.innertube.requests.player
import it.vfsfitvnm.innertube.utils.ExtraMediaIds
import it.vfsfitvnm.innertube.utils.soundCloudStreamUrl
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.MainActivity
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.AudioQuality
import it.vfsfitvnm.vimusic.enums.ExoPlayerDiskCacheMaxSize
import it.vfsfitvnm.vimusic.models.Event
import it.vfsfitvnm.vimusic.models.Format
import it.vfsfitvnm.vimusic.models.QueuedMediaItem
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.ui.styling.UiStrings
import it.vfsfitvnm.vimusic.utils.InvincibleService
import it.vfsfitvnm.vimusic.utils.preferredAppLanguage
import it.vfsfitvnm.vimusic.utils.withAppLanguage
import it.vfsfitvnm.vimusic.utils.RingBuffer
import it.vfsfitvnm.vimusic.utils.TimerJob
import it.vfsfitvnm.vimusic.utils.YouTubeRadio
import it.vfsfitvnm.vimusic.utils.activityPendingIntent
import it.vfsfitvnm.vimusic.utils.audioQualityKey
import it.vfsfitvnm.vimusic.utils.broadCastPendingIntent
import it.vfsfitvnm.vimusic.utils.crossfadeEnabledKey
import it.vfsfitvnm.vimusic.utils.exoPlayerDiskCacheMaxSizeKey
import it.vfsfitvnm.vimusic.utils.findNextMediaItemById
import it.vfsfitvnm.vimusic.utils.forcePlayFromBeginning
import it.vfsfitvnm.vimusic.utils.forceSeekToNext
import it.vfsfitvnm.vimusic.utils.forceSeekToPrevious
import it.vfsfitvnm.vimusic.utils.formatFor
import it.vfsfitvnm.vimusic.utils.getEnum
import it.vfsfitvnm.vimusic.utils.intent
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid12
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid13
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid7
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid8
import it.vfsfitvnm.vimusic.utils.isCharging
import it.vfsfitvnm.vimusic.utils.isInvincibilityEnabledKey
import it.vfsfitvnm.vimusic.utils.isOnUnmeteredNetwork
import it.vfsfitvnm.vimusic.utils.hasNetwork
import it.vfsfitvnm.vimusic.utils.resolveLyrics
import it.vfsfitvnm.vimusic.utils.isShowingThumbnailInLockscreenKey
import it.vfsfitvnm.vimusic.utils.khatmaMediaIdKey
import it.vfsfitvnm.vimusic.utils.khatmaPositionKey
import it.vfsfitvnm.vimusic.utils.lastPlayedMediaIdKey
import it.vfsfitvnm.vimusic.utils.lastPlayedPositionKey
import it.vfsfitvnm.vimusic.utils.Jellyfin
import it.vfsfitvnm.vimusic.utils.ListenBrainz
import it.vfsfitvnm.vimusic.utils.mediaItems
import it.vfsfitvnm.vimusic.utils.offlineModeKey
import it.vfsfitvnm.vimusic.utils.persistentQueueKey
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.queueLoopEnabledKey
import it.vfsfitvnm.vimusic.utils.resumePlaybackWhenDeviceConnectedKey
import it.vfsfitvnm.vimusic.utils.shouldBePlaying
import it.vfsfitvnm.vimusic.utils.playbackPitchKey
import it.vfsfitvnm.vimusic.utils.playbackSpeedKey
import it.vfsfitvnm.vimusic.utils.chargingOnlyDownloadKey
import it.vfsfitvnm.vimusic.utils.skipSilenceKey
import it.vfsfitvnm.vimusic.utils.startMediaForeground
import it.vfsfitvnm.vimusic.utils.timer
import it.vfsfitvnm.vimusic.utils.trackLoopEnabledKey
import it.vfsfitvnm.vimusic.utils.bassBoostKey
import it.vfsfitvnm.vimusic.utils.equalizerEnabledKey
import it.vfsfitvnm.vimusic.utils.equalizerPresetKey
import it.vfsfitvnm.vimusic.utils.lowPowerModeKey
import it.vfsfitvnm.vimusic.utils.playbackLookahead
import it.vfsfitvnm.vimusic.utils.videoModeKey
import it.vfsfitvnm.vimusic.utils.volumeNormalizationKey
import it.vfsfitvnm.vimusic.utils.wifiOnlyDownloadKey
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

@Suppress("DEPRECATION")
class PlayerService : InvincibleService(), Player.Listener, PlaybackStatsListener.Callback,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var mediaSession: Media3Session
    private lateinit var cache: SimpleCache
    private lateinit var downloadCache: SimpleCache
    private lateinit var player: ExoPlayer

    private var notificationManager: NotificationManager? = null

    private var radio: YouTubeRadio? = null

    private lateinit var bitmapProvider: BitmapProvider

    private val coroutineScope = CoroutineScope(Dispatchers.IO) + Job()
    private val sleepTimer = SleepTimer(coroutineScope)
    private val downloadStatuses = DownloadStatusHub.statuses
    private val streamUrlCache = StreamUrlCache<ResolvedStream>()
    private val streamResolver by lazy {
        StreamResolver(
            cache = streamUrlCache,
            quality = {
                if (preferences.getBoolean(lowPowerModeKey, false)) {
                    AudioQuality.Low
                } else {
                    preferences.getEnum(audioQualityKey, AudioQuality.Auto)
                }
            },
            player = { body -> Innertube.player(body) }
        )
    }

    private var volumeNormalizationJob: Job? = null

    private var isPersistentQueueEnabled = false
    private var isShowingThumbnailInLockscreen = true
    override var isInvincibilityEnabled = false

    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var playbackEffects: PlaybackEffects? = null
    private var effectsSessionId = -1
    private var loopStartMs = C.TIME_UNSET
    private var loopEndMs = C.TIME_UNSET

    private val binder = Binder()
    private var lastResumeSaveMs = 0L

    private var isNotificationStarted = false

    override val notificationId: Int
        get() = NotificationId

    private lateinit var notificationActionReceiver: NotificationActionReceiver

    private val strings: UiStrings
        get() = UiStrings(preferredAppLanguage())

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withAppLanguage())
    }

    override fun onBind(intent: Intent?): AndroidBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        bitmapProvider = BitmapProvider(
            bitmapSize = (256 * resources.displayMetrics.density).roundToInt(),
            colorProvider = { isSystemInDarkMode ->
                if (isSystemInDarkMode) Color.BLACK else Color.WHITE
            }
        )

        createNotificationChannel()

        preferences.registerOnSharedPreferenceChangeListener(this)

        val preferences = preferences
        isPersistentQueueEnabled = preferences.getBoolean(persistentQueueKey, false)
        isInvincibilityEnabled = preferences.getBoolean(isInvincibilityEnabledKey, false)
        isShowingThumbnailInLockscreen =
            preferences.getBoolean(isShowingThumbnailInLockscreenKey, false)

        val cacheEvictor = when (val size =
            preferences.getEnum(exoPlayerDiskCacheMaxSizeKey, ExoPlayerDiskCacheMaxSize.`2GB`)) {
            ExoPlayerDiskCacheMaxSize.Unlimited -> NoOpCacheEvictor()
            else -> LeastRecentlyUsedCacheEvictor(size.bytes)
        }

        // TODO: Remove in a future release
        val directory = cacheDir.resolve("exoplayer").also { directory ->
            if (directory.exists()) return@also

            directory.mkdir()

            cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.length == 1 && file.name.isDigitsOnly() || file.extension == "uid") {
                    if (!file.renameTo(directory.resolve(file.name))) {
                        file.deleteRecursively()
                    }
                }
            }

            filesDir.resolve("coil").deleteRecursively()
        }
        cache = PlaybackCaches.stream(this, directory, cacheEvictor)
        if (preferences.getInt(STREAM_CACHE_VERSION_KEY, 1) < STREAM_CACHE_VERSION) {
            // Older builds cached video (muxed) streams under the plain song key; audio mode
            // now uses that key, so mixing the two would corrupt playback. It is only a cache.
            runCatching { cache.keys.toList().forEach(cache::removeResource) }
            preferences.edit().putInt(STREAM_CACHE_VERSION_KEY, STREAM_CACHE_VERSION).apply()
        }
        downloadCache = PlaybackCaches.downloads(this)

        player = ExoPlayer.Builder(this, createRendersFactory(), createMediaSourceFactory())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setUsePlatformDiagnostics(false)
            .build()

        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

        player.repeatMode = when {
            preferences.getBoolean(trackLoopEnabledKey, false) -> Player.REPEAT_MODE_ONE
            preferences.getBoolean(queueLoopEnabledKey, true) -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }

        player.skipSilenceEnabled = preferences.getBoolean(skipSilenceKey, false)
        applyPlaybackParameters()
        applyPlaybackEffects(force = true)
        player.addListener(this)
        player.addAnalyticsListener(PlaybackStatsListener(false, this))

        maybeRestorePlayerQueue()

        mediaSession = Media3Session.Builder(this, SkipAwarePlayer(player))
            .setId("PlayerService")
            .setCallback(LibrarySessionCallback { song ->
                song.contentLength?.let { length -> downloadCache.isCached(song.song.id, 0, length) } == true
            })
            .build()

        notificationActionReceiver = NotificationActionReceiver(player)

        val filter = IntentFilter().apply {
            addAction(Action.play.value)
            addAction(Action.pause.value)
            addAction(Action.next.value)
            addAction(Action.previous.value)
        }

        if (isAtLeastAndroid13) {
            registerReceiver(notificationActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(notificationActionReceiver, filter)
        }

        maybeResumePlaybackWhenDeviceConnected()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (player.shouldBePlaying) {
            return
        }
        broadCastPendingIntent<NotificationDismissReceiver>().send()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        maybeSavePlayerQueue()
        maybeSaveResumePoint(force = true)

        preferences.unregisterOnSharedPreferenceChangeListener(this)

        player.removeListener(this)
        player.stop()
        player.release()

        unregisterReceiver(notificationActionReceiver)

        mediaSession.release()

        loudnessEnhancer?.release()
        playbackEffects?.release()
        playbackEffects = null

        super.onDestroy()
    }

    override fun shouldBeInvincible(): Boolean {
        return !player.shouldBePlaying
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (bitmapProvider.setDefaultBitmap() && player.currentMediaItem != null) {
            notificationManager?.notify(NotificationId, notification())
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        val totalPlayTimeMs = playbackStats.totalPlayTimeMs

        if (totalPlayTimeMs > 5000) {
            query {
                Database.incrementTotalPlayTimeMs(mediaItem.mediaId, totalPlayTimeMs)
            }
        }

        if (totalPlayTimeMs > 30000) {
            query {
                try {
                    Database.insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = System.currentTimeMillis(),
                            playTime = totalPlayTimeMs
                        )
                    )
                } catch (_: SQLException) {
                }
            }
            val scrobbleItem = mediaItem
            coroutineScope.launch {
                runCatching { ListenBrainz.submit(preferences, scrobbleItem, totalPlayTimeMs) }
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        player.volume = 1f
        maybeRecoverPlaybackError()
        maybeNormalizeVolume()
        maybeProcessRadio()
        prefetchUpcoming()

        if (mediaItem == null) {
            bitmapProvider.listener?.invoke(null)
        } else if (mediaItem.mediaMetadata.artworkUri == bitmapProvider.lastUri) {
            bitmapProvider.listener?.invoke(bitmapProvider.lastBitmap)
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            prefetchUpcoming()
        }
    }

    private fun prefetchUpcoming() {
        if (!::player.isInitialized || player.mediaItemCount == 0) return
        val timeline = player.currentTimeline
        if (timeline.windowCount == 0) return
        val start = player.currentMediaItemIndex.coerceAtLeast(0)
        val end = minOf(
            timeline.windowCount,
            start + playbackLookahead(preferences.getBoolean(lowPowerModeKey, false))
        )
        val ids = (start until end).map { index ->
            timeline.getWindow(index, Timeline.Window()).mediaItem.mediaId
        }
        streamResolver.prefetch(
            scope = coroutineScope,
            videoIds = ids,
            wantsVideo = preferences.getBoolean(videoModeKey, false),
            onResolved = ::publishResolved
        )
    }

    private fun publishResolved(stream: ResolvedStream) {
        val format = stream.format
        val body = stream.response
        reportVideoState(
            videoId = stream.videoId,
            hasVideo = body.hasMusicVideo(),
            playingVideo = stream.progressiveMuxed
        )
        query {
            Database.insert(
                Format(
                    songId = stream.videoId,
                    itag = format.itag,
                    mimeType = format.mimeType,
                    bitrate = format.bitrate,
                    loudnessDb = body.playerConfig?.audioConfig?.normalizedLoudnessDb,
                    contentLength = format.contentLength,
                    lastModified = format.lastModified
                )
            )
        }
        coroutineScope.launch(Dispatchers.Main) {
            val mediaItem = player.findNextMediaItemById(stream.videoId) ?: return@launch
            query { Database.insert(mediaItem) }
            val extras = mediaItem.mediaMetadata.extras ?: return@launch
            if (extras.getString("durationText") != null) return@launch
            val durationText = format.approxDurationMs
                ?.div(1000)
                ?.let(DateUtils::formatElapsedTime)
                ?.removePrefix("0")
                ?: return@launch
            extras.putString("durationText", durationText)
            query { Database.updateDurationText(stream.videoId, durationText) }
        }
    }

    private fun maybeRecoverPlaybackError() {
        if (player.playerError != null) {
            player.prepare()
        }
    }

    private fun maybeProcessRadio() {
        radio?.let { radio ->
            if (player.mediaItemCount - player.currentMediaItemIndex <= 3) {
                coroutineScope.launch(Dispatchers.Main) {
                    player.addMediaItems(radio.process())
                }
            }
        }
    }

    private fun maybeSavePlayerQueue() {
        if (!isPersistentQueueEnabled) return

        val mediaItems = player.currentTimeline.mediaItems
        val mediaItemIndex = player.currentMediaItemIndex
        val mediaItemPosition = player.currentPosition

        mediaItems.mapIndexed { index, mediaItem ->
            QueuedMediaItem(
                mediaItem = mediaItem,
                position = if (index == mediaItemIndex) mediaItemPosition else null
            )
        }.let { queuedMediaItems ->
            query {
                Database.clearQueue()
                Database.insert(queuedMediaItems)
            }
        }
    }

    private fun applyPlaybackParameters() {
        val speed = preferences.getFloat(playbackSpeedKey, 1f).coerceIn(0.5f, 2f)
        val pitch = preferences.getFloat(playbackPitchKey, 1f).coerceIn(0.5f, 2f)
        player.playbackParameters = PlaybackParameters(speed, pitch)
    }

    private fun maybeRestorePlayerQueue() {
        if (!isPersistentQueueEnabled) return

        coroutineScope.launch(Dispatchers.IO) {
            val queuedSong = Database.queue()
            if (queuedSong.isEmpty()) return@launch
            Database.clearQueue()

            val index = queuedSong.indexOfFirst { it.position != null }.coerceAtLeast(0)
            withContext(Dispatchers.Main) {
                player.setMediaItems(
                    queuedSong.map { mediaItem ->
                        mediaItem.mediaItem.buildUpon()
                            .setUri(mediaItem.mediaItem.mediaId)
                            .setCustomCacheKey(mediaItem.mediaItem.mediaId)
                            .build().apply {
                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                            }
                    },
                    index,
                    queuedSong[index].position ?: C.TIME_UNSET
                )
                player.prepare()

                val restoredNotification = notification() ?: return@withContext
                isNotificationStarted = true
                startForegroundService(this@PlayerService, intent<PlayerService>())
                this@PlayerService.startMediaForeground(NotificationId, restoredNotification)
            }
        }
    }

    private fun applyPlaybackEffects(force: Boolean = false) {
        if (!::player.isInitialized) return
        val session = player.audioSessionId
        if (!force && session == effectsSessionId) return
        playbackEffects?.release()
        playbackEffects = PlaybackEffects.attach(session, preferences)
        effectsSessionId = session
    }

    private fun maybeNormalizeVolume() {
        if (!preferences.getBoolean(volumeNormalizationKey, false)) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            player.volume = 1f
            return
        }

        if (loudnessEnhancer == null) {
            loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        }

        player.currentMediaItem?.mediaId?.let { songId ->
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = coroutineScope.launch(Dispatchers.Main) {
                Database.loudnessDb(songId).cancellable().collectLatest { loudnessDb ->
                    try {
                        loudnessEnhancer?.setTargetGain(-((loudnessDb ?: 0f) * 100).toInt() + 500)
                        loudnessEnhancer?.enabled = true
                    } catch (_: Exception) { }
                }
            }
        }
    }

    private fun maybeShowSongCoverInLockScreen() = Unit

    @SuppressLint("NewApi")
    private fun maybeResumePlaybackWhenDeviceConnected() {
        if (!isAtLeastAndroid6) return

        if (preferences.getBoolean(resumePlaybackWhenDeviceConnectedKey, false)) {
            if (audioManager == null) {
                audioManager = getSystemService(AUDIO_SERVICE) as AudioManager?
            }

            audioDeviceCallback = object : AudioDeviceCallback() {
                private fun canPlayMusic(audioDeviceInfo: AudioDeviceInfo): Boolean {
                    if (!audioDeviceInfo.isSink) return false

                    return audioDeviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }

                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    if (!player.isPlaying && addedDevices.any(::canPlayMusic)) {
                        player.play()
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = Unit
            }

            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, handler)

        } else {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
            audioDeviceCallback = null
        }
    }

    private fun sendOpenEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }

    private fun sendCloseEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
            }
        )
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY
            )
        ) {
            val notification = notification()

            if (notification == null) {
                isNotificationStarted = false
                makeInvincible(false)
                stopForeground(false)
                sendCloseEqualizerIntent()
                notificationManager?.cancel(NotificationId)
                return
            }

            if (!isNotificationStarted) {
                isNotificationStarted = true
                startForegroundService(this@PlayerService, intent<PlayerService>())
                startMediaForeground(NotificationId, notification)
            } else {
                notificationManager?.notify(NotificationId, notification)
            }

            applyPlaybackEffects()

            if (player.shouldBePlaying) {
                makeInvincible(false)
                sendOpenEqualizerIntent()
                if (!preferences.getBoolean(crossfadeEnabledKey, false)) {
                    player.volume = 1f
                }
            } else {
                makeInvincible(true)
                sendCloseEqualizerIntent()
            }

            PlayerWidgetProvider.update(
                this@PlayerService,
                player.mediaMetadata.title?.toString(),
                player.mediaMetadata.artist?.toString(),
                player.isPlaying
            )

            maybeCrossfade()
            maybeEnforceAbLoop()
            maybeSaveResumePoint()
        }
    }

    private fun maybeCrossfade() {
        if (preferences.getBoolean(lowPowerModeKey, false)) {
            if (player.volume != 1f) player.volume = 1f
            return
        }
        if (!preferences.getBoolean(crossfadeEnabledKey, false)) {
            return
        }
        val duration = player.duration
        if (duration == C.TIME_UNSET) return
        val remaining = duration - player.currentPosition
        player.volume = if (remaining in 1..3500L && player.hasNextMediaItem()) {
            (remaining / 3500f).coerceIn(0.12f, 1f)
        } else {
            1f
        }
    }

    private fun maybeEnforceAbLoop() {
        if (loopStartMs == C.TIME_UNSET || loopEndMs == C.TIME_UNSET) return
        if (loopEndMs <= loopStartMs) return
        if (player.currentPosition >= loopEndMs) {
            player.seekTo(loopStartMs)
        }
    }

    private fun maybeSaveResumePoint(force: Boolean = false) {
        val mediaItem = player.currentMediaItem ?: return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastResumeSaveMs < 5000L) return
        lastResumeSaveMs = now
        preferences.edit()
            .putString(lastPlayedMediaIdKey, mediaItem.mediaId)
            .putLong(lastPlayedPositionKey, player.currentPosition)
            .apply()
        maybeSaveKhatma()
    }

    private fun maybeSaveKhatma() {
        val title = player.mediaMetadata.title?.toString().orEmpty()
        val artist = player.mediaMetadata.artist?.toString().orEmpty()
        val haystack = "$title $artist"
        if (!haystack.contains("قرآن") && !haystack.contains("Quran", ignoreCase = true)) return
        preferences.edit()
            .putString(khatmaMediaIdKey, player.currentMediaItem?.mediaId)
            .putLong(khatmaPositionKey, player.currentPosition)
            .apply()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            persistentQueueKey -> isPersistentQueueEnabled =
                sharedPreferences.getBoolean(key, isPersistentQueueEnabled)

            volumeNormalizationKey -> {
                maybeNormalizeVolume()
                applyPlaybackEffects(force = true)
            }

            equalizerEnabledKey, equalizerPresetKey, bassBoostKey -> applyPlaybackEffects(force = true)

            resumePlaybackWhenDeviceConnectedKey -> maybeResumePlaybackWhenDeviceConnected()

            isInvincibilityEnabledKey -> isInvincibilityEnabled =
                sharedPreferences.getBoolean(key, isInvincibilityEnabled)

            skipSilenceKey -> player.skipSilenceEnabled = sharedPreferences.getBoolean(key, false)
            isShowingThumbnailInLockscreenKey -> {
                isShowingThumbnailInLockscreen = sharedPreferences.getBoolean(key, true)
                maybeShowSongCoverInLockScreen()
            }

            trackLoopEnabledKey, queueLoopEnabledKey -> {
                player.repeatMode = when {
                    preferences.getBoolean(trackLoopEnabledKey, false) -> Player.REPEAT_MODE_ONE
                    preferences.getBoolean(queueLoopEnabledKey, true) -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }
            }
        }
    }

    override fun notification(): Notification? {
        if (player.currentMediaItem == null) return null

        val artwork = bitmapProvider.bitmap
        val built = buildPlaybackNotification(artwork)

        bitmapProvider.load(player.mediaMetadata.artworkUri) { bitmap ->
            maybeShowSongCoverInLockScreen()
            notificationManager?.notify(NotificationId, buildPlaybackNotification(bitmap))
        }

        return built
    }

    private fun buildPlaybackNotification(artwork: Bitmap?): Notification {
        val playIntent = Action.play.pendingIntent
        val pauseIntent = Action.pause.pendingIntent
        val nextIntent = Action.next.pendingIntent
        val prevIntent = Action.previous.pendingIntent
        val mediaMetadata = player.mediaMetadata
        val playing = player.shouldBePlaying
        val accent = artwork
            ?.let { Palette.from(it).generate().getVibrantColor(0xFF3D5AFE.toInt()) }
            ?: 0xFF3D5AFE.toInt()

        val builder = if (isAtLeastAndroid8) {
            Notification.Builder(applicationContext, NotificationChannelId)
        } else {
            Notification.Builder(applicationContext)
        }
            .setContentTitle(mediaMetadata.title)
            .setContentText(
                listOfNotNull(
                    mediaMetadata.artist,
                    sleepTimer.millisLeft?.value?.let { left ->
                        "⏱ ${DateUtils.formatElapsedTime(left / 1000)}"
                    }
                ).joinToString(" · ")
            )
            .setSubText(player.playerError?.message)
            .setLargeIcon(artwork)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(player.playerError?.let { R.drawable.alert_circle } ?: R.drawable.app_icon)
            .setOngoing(true)
            .setColor(accent)
            .setContentIntent(activityPendingIntent<MainActivity>(
                flags = PendingIntent.FLAG_UPDATE_CURRENT
            ) {
                putExtra("expandPlayerBottomSheet", true)
            })
            .setDeleteIntent(broadCastPendingIntent<NotificationDismissReceiver>())
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(
                Notification.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(frameworkSessionToken())
            )
            .addAction(R.drawable.play_skip_back, strings.skipBack, prevIntent)
            .addAction(
                if (playing) R.drawable.pause else R.drawable.play,
                if (playing) strings.pause else strings.play,
                if (playing) pauseIntent else playIntent
            )
            .addAction(R.drawable.play_skip_forward, strings.skipForward, nextIntent)

        if (isAtLeastAndroid8) {
            builder.setColorized(true)
        }

        if (isAtLeastAndroid12) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        if (isAtLeastAndroid7) {
            val card = RemoteViews(packageName, R.layout.notification_player_card).apply {
                if (artwork != null) {
                    setImageViewBitmap(R.id.notification_artwork, artwork)
                }
                setTextViewText(R.id.notification_title, mediaMetadata.title ?: "")
                setTextViewText(R.id.notification_artist, mediaMetadata.artist ?: "")
                setImageViewResource(
                    R.id.notification_play,
                    if (playing) R.drawable.pause else R.drawable.play
                )
                setInt(R.id.notification_play, "setColorFilter", Color.WHITE)
                setInt(R.id.notification_prev, "setColorFilter", Color.WHITE)
                setInt(R.id.notification_next, "setColorFilter", Color.WHITE)
                setOnClickPendingIntent(
                    R.id.notification_play,
                    if (playing) pauseIntent else playIntent
                )
                setOnClickPendingIntent(R.id.notification_prev, prevIntent)
                setOnClickPendingIntent(R.id.notification_next, nextIntent)
            }
            builder.setCustomBigContentView(card)
        }

        val notification = builder.build()
        notification.extras.putBoolean("android.media.isPlayingMedia", playing)
        notification.extras.putString("android.substName", "MiMusic")
        return notification
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService()

        if (!isAtLeastAndroid8) return

        notificationManager?.run {
            if (getNotificationChannel(NotificationChannelId) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        NotificationChannelId,
                        strings.nowPlaying,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        setSound(null, null)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }

            if (getNotificationChannel(SleepTimerNotificationChannelId) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        SleepTimerNotificationChannelId,
                        strings.sleepTimer,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        setSound(null, null)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }
        }
    }

    private fun setDownloadStatus(mediaId: String, status: DownloadStatus) {
        DownloadStatusHub.set(mediaId, status)
    }

    private fun downloadedContentLength(mediaId: String): Long? {
        val metadataLength = ContentMetadata.getContentLength(downloadCache.getContentMetadata(mediaId))
        if (metadataLength != C.LENGTH_UNSET.toLong()) return metadataLength

        val cachedBytes = downloadCache.getCachedBytes(mediaId, 0, Long.MAX_VALUE)
        return cachedBytes.takeIf { it > 0L }
    }

    private fun downloadedMimeType(mediaId: String): String? =
        runCatching {
            runBlocking(Dispatchers.IO) { Database.format(mediaId).firstOrNull()?.mimeType }
        }.getOrNull()

    /** Fetches and stores the lyrics with the download so they are available offline. */
    private suspend fun saveLyricsForOffline(mediaItem: MediaItem, durationMs: Long) {
        runCatching {
            val stored = Database.lyrics(mediaItem.mediaId).firstOrNull()
            if (!stored?.fixed.isNullOrBlank() || !stored?.synced.isNullOrBlank()) return

            val resolved = resolveLyrics(
                mediaId = mediaItem.mediaId,
                title = mediaItem.mediaMetadata.title?.toString(),
                artist = mediaItem.mediaMetadata.artist?.toString(),
                durationMs = durationMs
            )
            if (resolved.hasText) {
                Database.upsert(
                    it.vfsfitvnm.vimusic.models.Lyrics(
                        songId = mediaItem.mediaId,
                        fixed = resolved.fixed,
                        synced = resolved.synced
                    )
                )
            }
        }
    }

    private fun isFullyDownloaded(mediaId: String): Boolean {
        val length = downloadedContentLength(mediaId) ?: return false
        return downloadCache.isCached(mediaId, 0, length)
    }

    private suspend fun resolvePlayableFormat(
        videoId: String,
        preferMuxed: Boolean = preferences.getBoolean(videoModeKey, false)
    ): Pair<it.vfsfitvnm.innertube.models.PlayerResponse, it.vfsfitvnm.innertube.models.PlayerResponse.StreamingData.AdaptiveFormat> {
        val response = Innertube.player(PlayerBody(videoId = videoId))?.getOrThrow()
            ?: throw PlayableFormatNotFoundException()

        when (response.playabilityStatus?.status) {
            "OK" -> Unit
            "LOGIN_REQUIRED" -> throw LoginRequiredException()
            "UNPLAYABLE" -> throw UnplayableException()
            else -> throw PlayableFormatNotFoundException()
        }

        val format = response.streamingData?.formatFor(
            preferences.getEnum(audioQualityKey, AudioQuality.Auto),
            preferMuxed = preferMuxed
        ) ?: throw PlayableFormatNotFoundException()
        if (format.url.isNullOrBlank()) throw PlayableFormatNotFoundException()

        return response to format
    }

    private fun createHttpDataSourceFactory(): DataSource.Factory {
        val browser = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(16000)
            .setReadTimeoutMs(8000)
            .setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            )
            .setDefaultRequestProperties(
                mapOf(
                    "Origin" to "https://www.youtube.com",
                    "Referer" to "https://www.youtube.com/"
                )
            )
        val youtube = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(16000)
            .setReadTimeoutMs(8000)
            .setUserAgent("com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip")

        return DataSource.Factory {
            HostSwitchDataSource(
                googlevideo = youtube.createDataSource(),
                other = browser.createDataSource(),
                local = PrivateHttpDataSource()
            )
        }
    }

    private fun createCacheDataSource(): DataSource.Factory {
        val downloadSourceFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(createHttpDataSourceFactory())
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val onlineFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(downloadSourceFactory)

        // Reads only from local storage (downloads first, then the playback cache) and
        // never touches the network: used for downloaded songs and offline playback.
        val offlineFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setCacheWriteDataSinkFactory(null)
            .setUpstreamDataSourceFactory(
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setCacheWriteDataSinkFactory(null)
                    .setUpstreamDataSourceFactory { PlaceholderDataSource.INSTANCE }
            )

        return DataSource.Factory {
            OfflineSwitchDataSource(
                offline = offlineFactory.createDataSource(),
                online = onlineFactory.createDataSource()
            )
        }
    }

    private fun offlineDataSpec(dataSpec: DataSpec, videoId: String): DataSpec =
        dataSpec.buildUpon()
            .setUri(Uri.Builder().scheme(OFFLINE_SCHEME).authority(videoId).build())
            .setKey(videoId)
            .build()

    private fun reportVideoState(videoId: String, hasVideo: Boolean?, playingVideo: Boolean) {
        coroutineScope.launch(Dispatchers.Main) {
            hasVideo?.let { binder.videoAvailability[videoId] = it }
            binder.videoStreams[videoId] = playingVideo
            if (player.currentMediaItem?.mediaId == videoId) binder.isCurrentVideo = playingVideo
        }
    }

    private data class ResolvedUri(
        val videoId: String,
        val uri: Uri,
        val progressiveMuxed: Boolean
    )

    private fun applyResolvedUri(dataSpec: DataSpec, resolved: ResolvedUri, chunkLength: Long): DataSpec {
        val withUri = dataSpec.withUri(resolved.uri)
        return if (resolved.progressiveMuxed) {
            withUri
        } else {
            withUri.subrange(dataSpec.uriPositionOffset, chunkLength)
        }
    }

    /** True when the source is a real music video, not a song with static artwork. */
    private fun it.vfsfitvnm.innertube.models.PlayerResponse.hasMusicVideo(): Boolean =
        streamingData?.muxedFallbackFormat != null &&
            videoDetails?.musicVideoType != "MUSIC_VIDEO_TYPE_ATV"

    private fun isProgressiveMuxed(
        format: it.vfsfitvnm.innertube.models.PlayerResponse.StreamingData.AdaptiveFormat
    ): Boolean {
        return !format.isAudioOnly ||
            format.itag == 18 ||
            format.itag == 22 ||
            format.mimeType.contains("video", ignoreCase = true)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun createDataSourceFactory(): DataSource.Factory {
        val chunkLength = 512 * 1024L
        val ringBuffer = RingBuffer<ResolvedUri?>(2) { null }

        return ResolvingDataSource.Factory(
            androidx.media3.datasource.DefaultDataSource.Factory(this, createCacheDataSource())
        ) { rawDataSpec ->
            // Video mode items carry VIDEO_KEY_SUFFIX in their cache key so audio and
            // video bytes never mix in the cache.
            val videoId = (rawDataSpec.key ?: error("A key must be set")).removeSuffix(VIDEO_KEY_SUFFIX)
            val dataSpec = rawDataSpec.buildUpon().setKey(videoId).build()
            val uri = dataSpec.uri
            if (
                videoId.startsWith("local:") ||
                uri.scheme == "content" ||
                uri.scheme == "file"
            ) {
                coroutineScope.launch(Dispatchers.Main) { binder.isCurrentVideo = false }
                return@Factory dataSpec
            }

            val extra = ExtraMediaIds.decode(videoId)
            if (extra != null) {
                return@Factory when (videoId) {
                    ringBuffer.getOrNull(0)?.videoId -> applyResolvedUri(dataSpec, ringBuffer.getOrNull(0)!!, chunkLength)
                    ringBuffer.getOrNull(1)?.videoId -> applyResolvedUri(dataSpec, ringBuffer.getOrNull(1)!!, chunkLength)
                    else -> {
                        val url = when (extra.first) {
                            ExtraMediaIds.SOUNDCLOUD_PREFIX -> soundCloudStreamUrl(extra.second)
                            ExtraMediaIds.PODCAST_PREFIX -> extra.second
                            ExtraMediaIds.JELLYFIN_PREFIX -> Jellyfin.streamUrl(preferences, extra.second)
                            else -> throw UnplayableException()
                        }
                        coroutineScope.launch(Dispatchers.Main) { binder.isCurrentVideo = false }
                        val resolved = ResolvedUri(videoId, url.toUri(), true)
                        ringBuffer.append(resolved)
                        applyResolvedUri(dataSpec, resolved, chunkLength)
                    }
                }
            }

            val fullyDownloaded = isFullyDownloaded(videoId)
            val requestedLength = dataSpec.length.takeIf { it != C.LENGTH_UNSET.toLong() } ?: 1L
            val hasPartialCache = isRangeCached(downloadCache, videoId, dataSpec.position, requestedLength) ||
                isRangeCached(cache, videoId, dataSpec.position, requestedLength)
            when (
                it.vfsfitvnm.vimusic.AppGraph.choosePlayback(
                    fullyDownloaded = fullyDownloaded,
                    offlineOrNoNetwork = preferences.getBoolean(offlineModeKey, false) || !hasNetwork(),
                    hasPartialCache = hasPartialCache
                )
            ) {
                it.vfsfitvnm.vimusic.utils.PlaybackChoice.Downloaded -> {
                    reportVideoState(
                        videoId = videoId,
                        hasVideo = null,
                        playingVideo = downloadedMimeType(videoId)?.startsWith("video") == true
                    )
                    return@Factory offlineDataSpec(dataSpec, videoId)
                }
                it.vfsfitvnm.vimusic.utils.PlaybackChoice.LocalCache ->
                    return@Factory offlineDataSpec(dataSpec, videoId)
                it.vfsfitvnm.vimusic.utils.PlaybackChoice.Unavailable -> throw UnplayableException()
                it.vfsfitvnm.vimusic.utils.PlaybackChoice.NeedNetwork -> Unit
            }

            val wantsVideo = preferences.getBoolean(videoModeKey, false)
            val streamKey = streamResolver.key(videoId, wantsVideo)
            val streamSpec = dataSpec.buildUpon().setKey(streamKey).build()
            val cached = streamResolver.peek(streamKey)
            val resolved = try {
                cached ?: streamResolver.resolveBlocking(videoId, wantsVideo).also(::publishResolved)
            } catch (cause: Exception) {
                it.vfsfitvnm.vimusic.utils.PlaybackLogStore.append(
                    "playback failed $videoId: ${cause.javaClass.simpleName} ${cause.message} ${it.vfsfitvnm.innertube.utils.PlayerLog.lastSummary}"
                )
                throw PlaybackException(
                    cause.message ?: it.vfsfitvnm.innertube.utils.PlayerLog.lastSummary,
                    cause,
                    PlaybackException.ERROR_CODE_REMOTE_ERROR
                )
            }
            applyResolvedUri(
                streamSpec,
                ResolvedUri(resolved.key, resolved.uri, resolved.progressiveMuxed),
                chunkLength
            )
        }
    }

    private fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory(), createExtractorsFactory())
    }

    private fun createExtractorsFactory(): ExtractorsFactory {
        return DefaultExtractorsFactory()
            .setMp4ExtractorFlags(Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
    }

    private fun createRendersFactory(): RenderersFactory {
        val audioSink: AudioSink = DefaultAudioSink.Builder(this)
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(false)
            .setAudioProcessorChain(
                DefaultAudioProcessorChain(
                    emptyArray(),
                    SilenceSkippingAudioProcessor(
                        /* minimumSilenceDurationUs = */ 2_000_000L,
                        /* silenceRetentionRatio = */ 0.05f,
                        /* maxSilenceToKeepDurationUs = */ 2_000_000L,
                        /* minVolumeToKeepPercentageWhenMuting = */ 0,
                        /* silenceThresholdLevel = */ 256
                    ),
                    SonicAudioProcessor()
                )
            )
            .build()

        return RenderersFactory { handler: Handler?, _, audioListener: AudioRendererEventListener?, _, _ ->
            arrayOf(
                MediaCodecVideoRenderer(this, MediaCodecSelector.DEFAULT),
                MediaCodecAudioRenderer(
                    this,
                    MediaCodecSelector.DEFAULT,
                    handler,
                    audioListener,
                    audioSink
                )
            )
        }
    }

    inner class Binder : AndroidBinder() {
        val player: ExoPlayer
            get() = this@PlayerService.player

        val cache: Cache
            get() = this@PlayerService.cache

        val downloadCache: Cache
            get() = this@PlayerService.downloadCache

        val frameworkSessionToken
            get() = this@PlayerService.frameworkSessionToken()

        var isCurrentVideo by mutableStateOf(false)
            internal set

        /** mediaId → whether YouTube has a real music video for it (learned on resolve). */
        val videoAvailability = mutableStateMapOf<String, Boolean>()

        /** mediaId → whether the stream being played for it contains video. */
        val videoStreams = mutableStateMapOf<String, Boolean>()

        fun isPlayingVideo(mediaId: String) = videoStreams[mediaId] == true

        fun hasVideo(mediaId: String) = videoAvailability[mediaId] == true

        /**
         * Switches between audio and video like YouTube Music's Song/Video toggle and
         * reloads the current item at the same position so the change is immediate.
         */
        fun setVideoMode(enabled: Boolean) {
            preferences.edit().putBoolean(videoModeKey, enabled).apply()

            val index = player.currentMediaItemIndex
            val item = player.currentMediaItem ?: return
            val mediaId = item.mediaId
            if (mediaId.startsWith("local:") || ExtraMediaIds.isExternal(mediaId)) return
            if (isFullyDownloaded(mediaId)) return

            val position = player.currentPosition
            val playWhenReady = player.playWhenReady
            val reloaded = item.buildUpon()
                .setCustomCacheKey(if (enabled) mediaId + VIDEO_KEY_SUFFIX else mediaId)
                .build()

            player.addMediaItem(index + 1, reloaded)
            player.seekTo(index + 1, position)
            player.removeMediaItem(index)
            player.playWhenReady = playWhenReady
            player.prepare()
        }

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = sleepTimer.millisLeft

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set

        fun setBitmapListener(listener: ((Bitmap?) -> Unit)?) {
            bitmapProvider.listener = listener
        }

        fun startSleepTimer(delayMillis: Long) {
            sleepTimer.start(delayMillis) {
                val notification = NotificationCompat
                    .Builder(this@PlayerService, SleepTimerNotificationChannelId)
                    .setContentTitle(strings.sleepTimerEnded)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setSmallIcon(R.drawable.app_icon)
                    .build()

                notificationManager?.notify(SleepTimerNotificationId, notification)

                stopSelf()
                exitProcess(0)
            }
        }

        fun cancelSleepTimer() {
            sleepTimer.cancel()
        }

        fun setupRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) =
            startRadio(endpoint = endpoint, justAdd = true)

        fun playRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) =
            startRadio(endpoint = endpoint, justAdd = false)

        private fun startRadio(endpoint: NavigationEndpoint.Endpoint.Watch?, justAdd: Boolean) {
            val videoId = endpoint?.videoId
            if (videoId != null && ExtraMediaIds.isExternal(videoId)) return
            radioJob?.cancel()
            radio = null
            YouTubeRadio(
                endpoint?.videoId,
                endpoint?.playlistId,
                endpoint?.playlistSetVideoId,
                endpoint?.params
            ).let {
                isLoadingRadio = true
                radioJob = coroutineScope.launch(Dispatchers.Main) {
                    if (justAdd) {
                        player.addMediaItems(it.process().drop(1))
                    } else {
                        player.forcePlayFromBeginning(it.process())
                    }
                    radio = it
                    isLoadingRadio = false
                }
            }
        }

        fun stopRadio() {
            isLoadingRadio = false
            radioJob?.cancel()
            radio = null
        }

        fun isDownloaded(mediaId: String): Boolean = isFullyDownloaded(mediaId)

        fun isAvailableOffline(mediaId: String, contentLength: Long): Boolean {
            return isFullyDownloaded(mediaId) || cache.isCached(mediaId, 0, contentLength)
        }

        fun downloadStatus(mediaId: String): DownloadStatus {
            return downloadStatuses.value[mediaId]
                ?: if (isFullyDownloaded(mediaId)) DownloadStatus.Completed else DownloadStatus.None
        }

        fun downloadStatusFlow(mediaId: String) = downloadStatuses
            .map { statuses ->
                statuses[mediaId]
                    ?: if (isFullyDownloaded(mediaId)) DownloadStatus.Completed else DownloadStatus.None
            }
            .distinctUntilChanged()

        fun setPlaybackSpeed(speed: Float) {
            preferences.edit().putFloat(playbackSpeedKey, speed.coerceIn(0.5f, 2f)).apply()
            applyPlaybackParameters()
        }

        fun setPlaybackPitch(pitch: Float) {
            preferences.edit().putFloat(playbackPitchKey, pitch.coerceIn(0.5f, 2f)).apply()
            applyPlaybackParameters()
        }

        fun downloadAll(mediaItems: List<MediaItem>) {
            mediaItems.forEach(::download)
        }

        fun setAbLoop(startMs: Long, endMs: Long) {
            loopStartMs = startMs
            loopEndMs = endMs
        }

        fun clearAbLoop() {
            loopStartMs = C.TIME_UNSET
            loopEndMs = C.TIME_UNSET
        }

        val hasAbLoop: Boolean
            get() = loopStartMs != C.TIME_UNSET && loopEndMs != C.TIME_UNSET

        fun exportDownload(mediaId: String, output: android.net.Uri) {
            coroutineScope.launch(Dispatchers.IO) {
                val length = downloadedContentLength(mediaId) ?: return@launch
                val source = CacheDataSource.Factory()
                    .setCache(this@PlayerService.downloadCache)
                    .setUpstreamDataSourceFactory(createHttpDataSourceFactory())
                    .createDataSource()
                val spec = DataSpec.Builder()
                    .setUri("https://youtube.com/watch?v=$mediaId".toUri())
                    .setKey(mediaId)
                    .setLength(length)
                    .build()
                runCatching {
                    source.open(spec)
                    contentResolver.openOutputStream(output)?.use { outputStream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = source.read(buffer, 0, buffer.size)
                            if (read == C.RESULT_END_OF_INPUT) break
                            outputStream.write(buffer, 0, read)
                        }
                    }
                }
                runCatching { source.close() }
            }
        }

        fun download(mediaItem: MediaItem) {
            val mediaId = mediaItem.mediaId
            if (mediaId.startsWith("local:") || ExtraMediaIds.isExternal(mediaId)) return
            if (isFullyDownloaded(mediaId)) return
            if (preferences.getBoolean(wifiOnlyDownloadKey, false) && !isOnUnmeteredNetwork()) {
                setDownloadStatus(mediaId, DownloadStatus.Failed)
                return
            }
            if (preferences.getBoolean(chargingOnlyDownloadKey, false) && !isCharging()) {
                setDownloadStatus(mediaId, DownloadStatus.Failed)
                return
            }
            setDownloadStatus(mediaId, DownloadStatus.Downloading)
            DownloadScheduler.enqueue(
                context = this@PlayerService,
                mediaId = mediaId,
                title = mediaItem.mediaMetadata.title?.toString(),
                artist = mediaItem.mediaMetadata.artist?.toString(),
                album = mediaItem.mediaMetadata.albumTitle?.toString(),
                thumbnail = mediaItem.mediaMetadata.artworkUri?.toString()
            )
        }

        fun removeDownload(mediaId: String) {
            DownloadScheduler.cancel(this@PlayerService, mediaId)
            this@PlayerService.downloadCache.removeResource(mediaId)
            setDownloadStatus(mediaId, DownloadStatus.None)
            query { Database.clearDownloadSize(mediaId) }
        }

        fun clearDownloads() {
            DownloadScheduler.cancelAll(this@PlayerService)
            this@PlayerService.downloadCache.keys.toList().forEach { key ->
                this@PlayerService.downloadCache.removeResource(key)
            }
            DownloadStatusHub.statuses.value = emptyMap()
        }
    }

    private class NotificationActionReceiver(private val player: Player) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Action.pause.value -> player.pause()
                Action.play.value -> player.play()
                Action.next.value -> player.forceSeekToNext()
                Action.previous.value -> player.forceSeekToPrevious()
            }
        }
    }

    class NotificationDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            context.stopService(context.intent<PlayerService>())
        }
    }

    @JvmInline
    private value class Action(val value: String) {
        context(Context)
        val pendingIntent: PendingIntent
            get() = PendingIntent.getBroadcast(
                this@Context,
                100,
                Intent(value).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT.or(if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)
            )

        companion object {
            val pause = Action("it.vfsfitvnm.vimusic.pause")
            val play = Action("it.vfsfitvnm.vimusic.play")
            val next = Action("it.vfsfitvnm.vimusic.next")
            val previous = Action("it.vfsfitvnm.vimusic.previous")
        }
    }


    private fun frameworkSessionToken(): android.media.session.MediaSession.Token {
        return mediaSession.platformToken
    }

    private companion object {
        const val NotificationId = 1001
        const val NotificationChannelId = "default_channel_id"

        const val SleepTimerNotificationId = 1002
        const val SleepTimerNotificationChannelId = "sleep_timer_channel_id"

        const val OFFLINE_SCHEME = "mimusic-offline"
        const val DOWNLOAD_CHUNK_LENGTH = 1024 * 1024L
        const val VIDEO_KEY_SUFFIX = "#video"
        const val STREAM_CACHE_VERSION_KEY = "streamCacheVersion"
        const val STREAM_CACHE_VERSION = 2

        fun isRangeCached(cache: Cache, key: String, position: Long, length: Long): Boolean {
            return cache.getCachedLength(key, position, length) > 0
        }
    }
}

private class HostSwitchDataSource(
    private val googlevideo: DataSource,
    private val other: DataSource,
    private val local: DataSource
) : DataSource {
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        googlevideo.addTransferListener(transferListener)
        other.addTransferListener(transferListener)
        local.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val host = dataSpec.uri.host.orEmpty()
        active = when {
            host.contains("googlevideo", ignoreCase = true) -> googlevideo
            dataSpec.uri.scheme.equals("http", ignoreCase = true) &&
                it.vfsfitvnm.vimusic.utils.CleartextPolicy.allows(dataSpec.uri.toString()) -> local
            else -> other
        }
        return active!!.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return active?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
    }

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> {
        return active?.responseHeaders ?: emptyMap()
    }

    override fun close() {
        active?.close()
        active = null
    }
}

/** Sends [PlayerService.OFFLINE_SCHEME] requests to local storage and the rest to the network chain. */
private class OfflineSwitchDataSource(
    private val offline: DataSource,
    private val online: DataSource
) : DataSource {
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        offline.addTransferListener(transferListener)
        online.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        active = if (dataSpec.uri.scheme == "mimusic-offline") offline else online
        return active!!.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return active?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
    }

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> {
        return active?.responseHeaders ?: emptyMap()
    }

    override fun close() {
        active?.close()
        active = null
    }
}

enum class DownloadStatus {
    None,
    Downloading,
    Completed,
    Failed
}
