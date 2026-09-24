package it.vfsfitvnm.vimusic.service

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

@UnstableApi
internal class HostSwitchDataSource(
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

/** Sends offline-scheme requests to local storage and the rest to the network chain. */
@UnstableApi
internal class OfflineSwitchDataSource(
    private val offline: DataSource,
    private val online: DataSource
) : DataSource {
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        offline.addTransferListener(transferListener)
        online.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        active = if (dataSpec.uri.scheme == OFFLINE_SCHEME) offline else online
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

    private companion object {
        const val OFFLINE_SCHEME = "mimusic-offline"
    }
}
