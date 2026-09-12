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
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.text.format.DateUtils
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
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
import androidx.media3.datasource.TransferListener
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.NavigationEndpoint
import it.vfsfitvnm.innertube.models.bodies.PlayerBody
import it.vfsfitvnm.innertube.requests.player
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.MainActivity
import it.vfsfitvnm.vimusic.R
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
import it.vfsfitvnm.vimusic.utils.broadCastPendingIntent
import it.vfsfitvnm.vimusic.utils.exoPlayerDiskCacheMaxSizeKey
import it.vfsfitvnm.vimusic.utils.findNextMediaItemById
import it.vfsfitvnm.vimusic.utils.forcePlayFromBeginning
import it.vfsfitvnm.vimusic.utils.forceSeekToNext
import it.vfsfitvnm.vimusic.utils.forceSeekToPrevious
import it.vfsfitvnm.vimusic.utils.getEnum
import it.vfsfitvnm.vimusic.utils.intent
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid12
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid13
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid7
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid8
import it.vfsfitvnm.vimusic.utils.isInvincibilityEnabledKey
import it.vfsfitvnm.vimusic.utils.isShowingThumbnailInLockscreenKey
import it.vfsfitvnm.vimusic.utils.mediaItems
import it.vfsfitvnm.vimusic.utils.persistentQueueKey
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.queueLoopEnabledKey
import it.vfsfitvnm.vimusic.utils.resumePlaybackWhenDeviceConnectedKey
import it.vfsfitvnm.vimusic.utils.shouldBePlaying
import it.vfsfitvnm.vimusic.utils.skipSilenceKey
import it.vfsfitvnm.vimusic.utils.startMediaForeground
import it.vfsfitvnm.vimusic.utils.timer
import it.vfsfitvnm.vimusic.utils.trackLoopEnabledKey
import it.vfsfitvnm.vimusic.utils.volumeNormalizationKey
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

@Suppress("DEPRECATION")
class PlayerService : InvincibleService(), Player.Listener, PlaybackStatsListener.Callback,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var mediaSession: MediaSession
    private lateinit var cache: SimpleCache
    private lateinit var downloadCache: SimpleCache
    private lateinit var player: ExoPlayer

    private val stateBuilder = PlaybackState.Builder()
        .setActions(
            PlaybackState.ACTION_PLAY
                    or PlaybackState.ACTION_PAUSE
                    or PlaybackState.ACTION_PLAY_PAUSE
                    or PlaybackState.ACTION_STOP
                    or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackState.ACTION_SKIP_TO_NEXT
                    or PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM
                    or PlaybackState.ACTION_SEEK_TO
                    or PlaybackState.ACTION_REWIND
        )

    private val metadataBuilder = MediaMetadata.Builder()

    private var notificationManager: NotificationManager? = null

    private var timerJob: TimerJob? = null

    private var radio: YouTubeRadio? = null

    private lateinit var bitmapProvider: BitmapProvider

    private val coroutineScope = CoroutineScope(Dispatchers.IO) + Job()
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val downloadStatuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())

    private var volumeNormalizationJob: Job? = null

    private var isPersistentQueueEnabled = false
    private var isShowingThumbnailInLockscreen = true
    override var isInvincibilityEnabled = false

    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val binder = Binder()

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
        val databaseProvider = StandaloneDatabaseProvider(this)
        cache = SimpleCache(directory, cacheEvictor, databaseProvider)
        downloadCache = SimpleCache(
            filesDir.resolve("downloads").apply { mkdirs() },
            NoOpCacheEvictor(),
            databaseProvider
        )

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

        player.repeatMode = when {
            preferences.getBoolean(trackLoopEnabledKey, false) -> Player.REPEAT_MODE_ONE
            preferences.getBoolean(queueLoopEnabledKey, true) -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }

        player.skipSilenceEnabled = preferences.getBoolean(skipSilenceKey, false)
        player.addListener(this)
        player.addAnalyticsListener(PlaybackStatsListener(false, this))

        maybeRestorePlayerQueue()

        mediaSession = MediaSession(baseContext, "PlayerService")
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(SessionCallback(player))
        mediaSession.setPlaybackState(stateBuilder.build())
        mediaSession.isActive = true

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

        preferences.unregisterOnSharedPreferenceChangeListener(this)

        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()

        player.removeListener(this)
        player.stop()
        player.release()

        unregisterReceiver(notificationActionReceiver)

        mediaSession.isActive = false
        mediaSession.release()
        cache.release()
        downloadCache.release()

        loudnessEnhancer?.release()

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
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        maybeRecoverPlaybackError()
        maybeNormalizeVolume()
        maybeProcessRadio()

        if (mediaItem == null) {
            bitmapProvider.listener?.invoke(null)
        } else if (mediaItem.mediaMetadata.artworkUri == bitmapProvider.lastUri) {
            bitmapProvider.listener?.invoke(bitmapProvider.lastBitmap)
        }

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            updateMediaSessionQueue(player.currentTimeline)
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            updateMediaSessionQueue(timeline)
        }
    }

    private fun updateMediaSessionQueue(timeline: Timeline) {
        val builder = MediaDescription.Builder()

        val currentMediaItemIndex = player.currentMediaItemIndex
        val lastIndex = timeline.windowCount - 1
        var startIndex = currentMediaItemIndex - 7
        var endIndex = currentMediaItemIndex + 7

        if (startIndex < 0) {
            endIndex -= startIndex
        }

        if (endIndex > lastIndex) {
            startIndex -= (endIndex - lastIndex)
            endIndex = lastIndex
        }

        startIndex = startIndex.coerceAtLeast(0)

        mediaSession.setQueue(
            List(endIndex - startIndex + 1) { index ->
                val mediaItem = timeline.getWindow(index + startIndex, Timeline.Window()).mediaItem
                MediaSession.QueueItem(
                    builder
                        .setMediaId(mediaItem.mediaId)
                        .setTitle(mediaItem.mediaMetadata.title)
                        .setSubtitle(mediaItem.mediaMetadata.artist)
                        .setIconUri(mediaItem.mediaMetadata.artworkUri)
                        .build(),
                    (index + startIndex).toLong()
                )
            }
        )
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

    private fun maybeRestorePlayerQueue() {
        if (!isPersistentQueueEnabled) return

        query {
            val queuedSong = Database.queue()
            Database.clearQueue()

            if (queuedSong.isEmpty()) return@query

            val index = queuedSong.indexOfFirst { it.position != null }.coerceAtLeast(0)

            runBlocking(Dispatchers.Main) {
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

                val restoredNotification = notification() ?: return@runBlocking
                isNotificationStarted = true
                startForegroundService(this@PlayerService, intent<PlayerService>())
                this@PlayerService.startMediaForeground(NotificationId, restoredNotification)
            }
        }
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

    private fun maybeShowSongCoverInLockScreen() {
        val bitmap =
            if (isAtLeastAndroid13 || isShowingThumbnailInLockscreen) bitmapProvider.bitmap else null

        metadataBuilder
            .putText(MediaMetadata.METADATA_KEY_TITLE, player.mediaMetadata.title)
            .putText(MediaMetadata.METADATA_KEY_ARTIST, player.mediaMetadata.artist)
            .putText(MediaMetadata.METADATA_KEY_ALBUM, player.mediaMetadata.albumTitle)
            .putText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, player.mediaMetadata.title)
            .putText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, player.mediaMetadata.artist)
            .putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
            .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, bitmap)

        if (player.duration != C.TIME_UNSET) {
            metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, player.duration)
        }

        if (isAtLeastAndroid13 && player.currentMediaItemIndex == 0) {
            metadataBuilder.putText(
                MediaMetadata.METADATA_KEY_TITLE,
                "${player.mediaMetadata.title} "
            )
        }

        mediaSession.setMetadata(metadataBuilder.build())
    }

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

    private val Player.androidPlaybackState: Int
        get() = when (playbackState) {
            Player.STATE_BUFFERING -> if (playWhenReady) PlaybackState.STATE_BUFFERING else PlaybackState.STATE_PAUSED
            Player.STATE_READY -> if (playWhenReady) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackState.STATE_STOPPED
            Player.STATE_IDLE -> PlaybackState.STATE_NONE
            else -> PlaybackState.STATE_NONE
        }

    override fun onEvents(player: Player, events: Player.Events) {
        if (player.duration != C.TIME_UNSET) {
            mediaSession.setMetadata(
                metadataBuilder
                    .putText(MediaMetadata.METADATA_KEY_TITLE, player.mediaMetadata.title)
                    .putText(MediaMetadata.METADATA_KEY_ARTIST, player.mediaMetadata.artist)
                    .putText(MediaMetadata.METADATA_KEY_ALBUM, player.mediaMetadata.albumTitle)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, player.duration)
                    .build()
            )
        }

        stateBuilder
            .setState(player.androidPlaybackState, player.currentPosition, 1f)
            .setBufferedPosition(player.bufferedPosition)

        mediaSession.setPlaybackState(stateBuilder.build())

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

            if (player.shouldBePlaying) {
                makeInvincible(false)
                sendOpenEqualizerIntent()
            } else {
                makeInvincible(true)
                sendCloseEqualizerIntent()
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            persistentQueueKey -> isPersistentQueueEnabled =
                sharedPreferences.getBoolean(key, isPersistentQueueEnabled)

            volumeNormalizationKey -> maybeNormalizeVolume()

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
            .setContentText(mediaMetadata.artist)
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
                    .setMediaSession(mediaSession.sessionToken)
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
        downloadStatuses.update { current ->
            if (status == DownloadStatus.None) current - mediaId else current + (mediaId to status)
        }
    }

    private fun downloadedContentLength(mediaId: String): Long? {
        val metadataLength = ContentMetadata.getContentLength(downloadCache.getContentMetadata(mediaId))
        if (metadataLength != C.LENGTH_UNSET.toLong()) return metadataLength

        val cachedBytes = downloadCache.getCachedBytes(mediaId, 0, Long.MAX_VALUE)
        return cachedBytes.takeIf { it > 0L }
    }

    private fun isFullyDownloaded(mediaId: String): Boolean {
        val length = downloadedContentLength(mediaId) ?: return false
        return downloadCache.isCached(mediaId, 0, length)
    }

    private suspend fun resolvePlayableFormat(
        videoId: String
    ): Pair<it.vfsfitvnm.innertube.models.PlayerResponse, it.vfsfitvnm.innertube.models.PlayerResponse.StreamingData.AdaptiveFormat> {
        val response = Innertube.player(PlayerBody(videoId = videoId))?.getOrThrow()
            ?: throw PlayableFormatNotFoundException()

        when (response.playabilityStatus?.status) {
            "OK" -> Unit
            "LOGIN_REQUIRED" -> throw LoginRequiredException()
            "UNPLAYABLE" -> throw UnplayableException()
            else -> throw PlayableFormatNotFoundException()
        }

        val format = response.streamingData?.playableFormat ?: throw PlayableFormatNotFoundException()
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
                other = browser.createDataSource()
            )
        }
    }

    private fun createCacheDataSource(): DataSource.Factory {
        val downloadSourceFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(createHttpDataSourceFactory())
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(downloadSourceFactory)
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val chunkLength = 512 * 1024L
        val ringBuffer = RingBuffer<Pair<String, Uri>?>(2) { null }

        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val videoId = dataSpec.key ?: error("A key must be set")

            if (
                isRangeCached(downloadCache, videoId, dataSpec.position, chunkLength) ||
                isRangeCached(cache, videoId, dataSpec.position, chunkLength) ||
                isFullyDownloaded(videoId)
            ) {
                dataSpec
            } else {
                when (videoId) {
                    ringBuffer.getOrNull(0)?.first -> dataSpec.withUri(ringBuffer.getOrNull(0)!!.second)
                    ringBuffer.getOrNull(1)?.first -> dataSpec.withUri(ringBuffer.getOrNull(1)!!.second)
                    else -> {
                        val urlResult = runBlocking(Dispatchers.IO) {
                            it.vfsfitvnm.vimusic.utils.PlaybackLogStore.append("player request $videoId")
                            Innertube.player(PlayerBody(videoId = videoId))
                        }?.mapCatching { body ->
                            val returnedVideoId = body.videoDetails?.videoId
                            if (returnedVideoId != null && returnedVideoId != videoId) {
                                throw VideoIdMismatchException()
                            }

                            when (val status = body.playabilityStatus?.status) {
                                "OK" -> body.streamingData?.playableFormat?.let { format ->
                                    val mediaItem = runBlocking(Dispatchers.Main) {
                                        player.findNextMediaItemById(videoId)
                                    }

                                    if (mediaItem?.mediaMetadata?.extras?.getString("durationText") == null) {
                                        format.approxDurationMs?.div(1000)
                                            ?.let(DateUtils::formatElapsedTime)?.removePrefix("0")
                                            ?.let { durationText ->
                                                mediaItem?.mediaMetadata?.extras?.putString(
                                                    "durationText",
                                                    durationText
                                                )
                                                Database.updateDurationText(videoId, durationText)
                                            }
                                    }

                                    query {
                                        mediaItem?.let(Database::insert)

                                        Database.insert(
                                            it.vfsfitvnm.vimusic.models.Format(
                                                songId = videoId,
                                                itag = format.itag,
                                                mimeType = format.mimeType,
                                                bitrate = format.bitrate,
                                                loudnessDb = body.playerConfig?.audioConfig?.normalizedLoudnessDb,
                                                contentLength = format.contentLength,
                                                lastModified = format.lastModified
                                            )
                                        )
                                    }

                                    it.vfsfitvnm.vimusic.utils.PlaybackLogStore.append(
                                        "resolved $videoId itag=${format.itag} mime=${format.mimeType}"
                                    )
                                    format.url
                                } ?: throw PlayableFormatNotFoundException()

                                "UNPLAYABLE" -> throw UnplayableException()
                                "LOGIN_REQUIRED" -> throw LoginRequiredException()
                                else -> throw PlaybackException(
                                    status,
                                    null,
                                    PlaybackException.ERROR_CODE_REMOTE_ERROR
                                )
                            }
                        }

                        urlResult?.getOrThrow()?.let { url ->
                            ringBuffer.append(videoId to url.toUri())
                            dataSpec.withUri(url.toUri())
                                .subrange(dataSpec.uriPositionOffset, chunkLength)
                        } ?: run {
                            val cause = urlResult?.exceptionOrNull()
                            it.vfsfitvnm.vimusic.utils.PlaybackLogStore.append(
                                "playback failed $videoId: ${cause?.javaClass?.simpleName} ${cause?.message} ${it.vfsfitvnm.innertube.utils.PlayerLog.lastSummary}"
                            )
                            throw PlaybackException(
                                cause?.message ?: it.vfsfitvnm.innertube.utils.PlayerLog.lastSummary,
                                cause,
                                PlaybackException.ERROR_CODE_REMOTE_ERROR
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory(), createExtractorsFactory())
    }

    private fun createExtractorsFactory(): ExtractorsFactory {
        return ExtractorsFactory {
            arrayOf(MatroskaExtractor(), FragmentedMp4Extractor(), Mp4Extractor())
        }
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

        val mediaSession
            get() = this@PlayerService.mediaSession

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = timerJob?.millisLeft

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set

        fun setBitmapListener(listener: ((Bitmap?) -> Unit)?) {
            bitmapProvider.listener = listener
        }

        fun startSleepTimer(delayMillis: Long) {
            timerJob?.cancel()

            timerJob = coroutineScope.timer(delayMillis) {
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
            timerJob?.cancel()
            timerJob = null
        }

        fun setupRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) =
            startRadio(endpoint = endpoint, justAdd = true)

        fun playRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) =
            startRadio(endpoint = endpoint, justAdd = false)

        private fun startRadio(endpoint: NavigationEndpoint.Endpoint.Watch?, justAdd: Boolean) {
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

        fun download(mediaItem: MediaItem) {
            val mediaId = mediaItem.mediaId
            if (downloadJobs.containsKey(mediaId) || isFullyDownloaded(mediaId)) return

            setDownloadStatus(mediaId, DownloadStatus.Downloading)
            downloadJobs[mediaId] = coroutineScope.launch(Dispatchers.IO) {
                val result = runCatching {
                    val (response, format) = resolvePlayableFormat(mediaId)
                    val url = format.url ?: throw PlayableFormatNotFoundException()

                    query {
                        Database.insert(mediaItem)
                        Database.insert(
                            Format(
                                songId = mediaId,
                                itag = format.itag,
                                mimeType = format.mimeType,
                                bitrate = format.bitrate,
                                loudnessDb = response.playerConfig?.audioConfig?.normalizedLoudnessDb,
                                contentLength = format.contentLength,
                                lastModified = format.lastModified
                            )
                        )
                    }

                    CacheWriter(
                        CacheDataSource.Factory()
                            .setCache(this@PlayerService.downloadCache)
                            .setUpstreamDataSourceFactory(createHttpDataSourceFactory())
                            .createDataSource(),
                        DataSpec.Builder()
                            .setUri(url)
                            .setKey(mediaId)
                            .setLength(format.contentLength ?: C.LENGTH_UNSET.toLong())
                            .build(),
                        null,
                        null
                    ).cache()

                    val storedLength = downloadedContentLength(mediaId)
                        ?: throw PlayableFormatNotFoundException()

                    downloadCache.applyContentMetadataMutations(
                        mediaId,
                        ContentMetadataMutations().setContentLength(storedLength)
                    )

                    query {
                        Database.insert(
                            Format(
                                songId = mediaId,
                                itag = format.itag,
                                mimeType = format.mimeType,
                                bitrate = format.bitrate,
                                loudnessDb = response.playerConfig?.audioConfig?.normalizedLoudnessDb,
                                contentLength = storedLength,
                                lastModified = format.lastModified
                            )
                        )
                    }
                }

                downloadJobs.remove(mediaId)
                setDownloadStatus(
                    mediaId,
                    if (result.isSuccess && isFullyDownloaded(mediaId)) {
                        DownloadStatus.Completed
                    } else {
                        result.exceptionOrNull()?.printStackTrace()
                        DownloadStatus.Failed
                    }
                )
            }
        }

        fun removeDownload(mediaId: String) {
            downloadJobs.remove(mediaId)?.cancel()
            this@PlayerService.downloadCache.removeResource(mediaId)
            setDownloadStatus(mediaId, DownloadStatus.None)
        }
    }

    private class SessionCallback(private val player: Player) : MediaSession.Callback() {
        override fun onPlay() = player.play()
        override fun onPause() = player.pause()
        override fun onSkipToPrevious() = runCatching(player::forceSeekToPrevious).let { }
        override fun onSkipToNext() = runCatching(player::forceSeekToNext).let { }
        override fun onSeekTo(pos: Long) = player.seekTo(pos)
        override fun onStop() = player.pause()
        override fun onRewind() = player.seekToDefaultPosition()
        override fun onSkipToQueueItem(id: Long) = runCatching { player.seekToDefaultPosition(id.toInt()) }.let { }
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

    private companion object {
        const val NotificationId = 1001
        const val NotificationChannelId = "default_channel_id"

        const val SleepTimerNotificationId = 1002
        const val SleepTimerNotificationChannelId = "sleep_timer_channel_id"

        fun isRangeCached(cache: Cache, key: String, position: Long, length: Long): Boolean {
            return cache.getCachedLength(key, position, length) > 0
        }
    }
}

private class HostSwitchDataSource(
    private val googlevideo: DataSource,
    private val other: DataSource
) : DataSource {
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        googlevideo.addTransferListener(transferListener)
        other.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val host = dataSpec.uri.host.orEmpty()
        active = if (host.contains("googlevideo", ignoreCase = true)) googlevideo else other
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
