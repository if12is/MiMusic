package it.vfsfitvnm.vimusic.service

import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
internal class LockscreenArtworkLoader(
    private val delegate: BitmapLoader,
    private val showArtwork: () -> Boolean
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = delegate.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        if (!showArtwork()) return Futures.immediateCancelledFuture()
        return delegate.decodeBitmap(data)
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        if (!showArtwork()) return Futures.immediateCancelledFuture()
        return delegate.loadBitmap(uri)
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        if (!showArtwork()) return Futures.immediateCancelledFuture()
        return delegate.loadBitmapFromMetadata(metadata)
    }
}
