package it.vfsfitvnm.vimusic.service

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.cast.CastPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import it.vfsfitvnm.vimusic.utils.PlaybackLogStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@UnstableApi
object DeviceCast {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var castPlayer: CastPlayer? = null
    private var listening = false
    private var pending: Pending? = null
    private var appContext: Context? = null
    private var localServer: LocalCastServer? = null

    @Suppress("TooGenericExceptionCaught")
    fun request(
        context: Context,
        localPlayer: Player,
        mediaItem: MediaItem,
        resolve: suspend (String) -> Uri?
    ): Boolean {
        val services = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (services != ConnectionResult.SUCCESS) return false
        appContext = context.applicationContext
        val castContext = try {
            CastContext.getSharedInstance(context)
        } catch (e: RuntimeException) {
            PlaybackLogStore.append("cast unavailable ${e.javaClass.simpleName}: ${e.message}")
            return false
        }
        pending = Pending(localPlayer, mediaItem, resolve)
        listen(castContext)
        val session = castContext.sessionManager.currentCastSession
        if (session != null && session.isConnected) {
            scope.launch { load(castContext) }
            return true
        }
        val selector = castContext.mergedSelector ?: return false
        return try {
            MediaRouteButton(context).apply {
                routeSelector = selector
            }.showDialog()
            true
        } catch (e: IllegalStateException) {
            PlaybackLogStore.append("cast dialog ${e.message}")
            false
        } catch (e: IllegalArgumentException) {
            PlaybackLogStore.append("cast dialog ${e.message}")
            false
        }
    }

    private fun listen(castContext: CastContext) {
        if (listening) return
        listening = true
        castContext.sessionManager.addSessionManagerListener(sessionListener(castContext), CastSession::class.java)
    }

    private fun sessionListener(castContext: CastContext) = object : SessionManagerListener<CastSession> {
        override fun onSessionEnded(session: CastSession, error: Int) = stopLocal()
        override fun onSessionEnding(session: CastSession) = stopLocal()
        override fun onSessionResumeFailed(session: CastSession, error: Int) = stopLocal()
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            scope.launch { load(castContext) }
        }
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionStartFailed(session: CastSession, error: Int) = stopLocal()
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            scope.launch { load(castContext) }
        }
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
    }

    private suspend fun load(castContext: CastContext) {
        val request = pending ?: return
        val uri = resolveUri(request) ?: return
        request.localPlayer.pause()
        val item = request.mediaItem.buildUpon().setUri(uri).build()
        val player = castPlayer ?: CastPlayer(castContext).also { castPlayer = it }
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    private suspend fun resolveUri(request: Pending): Uri? {
        val resolved = try {
            request.resolve(request.mediaItem.mediaId)
        } catch (e: IllegalStateException) {
            PlaybackLogStore.append("cast resolve ${e.message}")
            null
        } catch (e: java.io.IOException) {
            PlaybackLogStore.append("cast resolve ${e.message}")
            null
        }
        if (resolved != null) return resolved
        val direct = request.mediaItem.localConfiguration?.uri ?: return null
        val scheme = direct.scheme?.lowercase()
        if (scheme == "https" || scheme == "http") return direct
        if (scheme == "file" || scheme == "content" || request.mediaItem.mediaId.startsWith("local:")) {
            return serveLocal(direct)
        }
        return null
    }

    private suspend fun serveLocal(uri: Uri): Uri? {
        val context = appContext ?: return null
        val server = localServer ?: LocalCastServer(context).also { localServer = it }
        val url = withContext(Dispatchers.IO) { server.urlFor(uri) } ?: return null
        return url.toUri()
    }

    private fun stopLocal() {
        localServer?.stop()
    }

    private data class Pending(
        val localPlayer: Player,
        val mediaItem: MediaItem,
        val resolve: suspend (String) -> Uri?
    )
}
