package it.vfsfitvnm.vimusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.ui.styling.UiStrings
import it.vfsfitvnm.vimusic.utils.chargingOnlyDownloadKey
import it.vfsfitvnm.vimusic.utils.preferredAppLanguage
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.wifiOnlyDownloadKey
import kotlinx.coroutines.CancellationException
import androidx.work.Constraints

object DownloadStatusHub {
    val statuses = kotlinx.coroutines.flow.MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val progress = kotlinx.coroutines.flow.MutableStateFlow<Map<String, Int>>(emptyMap())

    fun set(mediaId: String, status: DownloadStatus, percent: Int? = null) {
        statuses.value = statuses.value.let { current ->
            if (status == DownloadStatus.None) current - mediaId else current + (mediaId to status)
        }
        progress.value = if (status == DownloadStatus.Downloading) {
            val shown = (percent ?: progress.value[mediaId] ?: 0).coerceIn(0, 100)
            progress.value + (mediaId to shown)
        } else {
            progress.value - mediaId
        }
    }

    fun clear() {
        statuses.value = emptyMap()
        progress.value = emptyMap()
    }
}

object DownloadScheduler {
    const val TAG = "song-download"
    const val KEY_MEDIA_ID = "mediaId"
    const val KEY_TITLE = "title"
    const val KEY_ARTIST = "artist"
    const val KEY_ALBUM = "album"
    const val KEY_THUMBNAIL = "thumbnail"

    fun enqueue(
        context: Context,
        mediaId: String,
        title: String?,
        artist: String?,
        album: String?,
        thumbnail: String?
    ) {
        val preferences = context.preferences
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (preferences.getBoolean(wifiOnlyDownloadKey, false)) {
                    NetworkType.UNMETERED
                } else {
                    NetworkType.CONNECTED
                }
            )
            .setRequiresCharging(preferences.getBoolean(chargingOnlyDownloadKey, false))
            .build()
        val request = OneTimeWorkRequestBuilder<SongDownloadWorker>()
            .setInputData(
                workDataOf(
                    KEY_MEDIA_ID to mediaId,
                    KEY_TITLE to title,
                    KEY_ARTIST to artist,
                    KEY_ALBUM to album,
                    KEY_THUMBNAIL to thumbnail
                )
            )
            .setConstraints(constraints)
            .addTag(TAG)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(mediaId),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context, mediaId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(mediaId))
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }

    fun workName(mediaId: String) = "download:$mediaId"
}

@UnstableApi
class SongDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        val mediaId = inputData.getString(DownloadScheduler.KEY_MEDIA_ID) ?: return Result.failure()
        val title = inputData.getString(DownloadScheduler.KEY_TITLE)
        val strings = UiStrings(applicationContext.preferredAppLanguage())
        DownloadStatusHub.set(mediaId, DownloadStatus.Downloading)
        return try {
            setProgressNotification(mediaId, title ?: strings.downloading, 0, strings.downloading)
            SongDownloader.download(
                context = applicationContext,
                cache = PlaybackCaches.downloads(applicationContext),
                mediaId = mediaId,
                title = title,
                artist = inputData.getString(DownloadScheduler.KEY_ARTIST),
                album = inputData.getString(DownloadScheduler.KEY_ALBUM),
                thumbnail = inputData.getString(DownloadScheduler.KEY_THUMBNAIL)
            ) { percent ->
                DownloadStatusHub.set(mediaId, DownloadStatus.Downloading, percent)
                setProgressNotification(mediaId, title ?: strings.downloading, percent, "$percent%")
            }
            DownloadStatusHub.set(mediaId, DownloadStatus.Completed)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: UnplayableException) {
            failDownload(mediaId, error, deletePartial = true)
        } catch (error: LoginRequiredException) {
            failDownload(mediaId, error, deletePartial = true)
        } catch (error: Exception) {
            failDownload(mediaId, error, deletePartial = false)
        }
    }

    private fun failDownload(mediaId: String, error: Exception, deletePartial: Boolean): Result {
        if (deletePartial) {
            runCatching { PlaybackCaches.downloads(applicationContext).removeResource(mediaId) }
        }
        DownloadStatusHub.set(mediaId, DownloadStatus.Failed)
        it.vfsfitvnm.vimusic.utils.PlaybackLogStore.append(
            "download failed $mediaId: ${error.javaClass.simpleName} ${error.message}"
        )
        return Result.failure()
    }

    private fun setProgressNotification(mediaId: String, title: String, percent: Int, text: String) {
        val notification = notification(title, text, percent)
        val id = notificationId(mediaId)
        val info = if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
        runCatching { setForegroundAsync(info) }
    }

    private fun notification(title: String, text: String, percent: Int): Notification {
        ensureChannel()
        return NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.drawable.download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, percent == 0)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notificationId(mediaId: String) = 2_000 + (mediaId.hashCode() and 0x0FFF)

    companion object {
        private const val CHANNEL = "download_channel"
    }
}
