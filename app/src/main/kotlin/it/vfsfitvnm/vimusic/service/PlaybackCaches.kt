package it.vfsfitvnm.vimusic.service

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object PlaybackCaches {
    private val lock = Any()
    private var databaseProvider: StandaloneDatabaseProvider? = null
    private var stream: SimpleCache? = null
    private var downloads: SimpleCache? = null

    private fun database(context: Context): StandaloneDatabaseProvider = synchronized(lock) {
        databaseProvider ?: StandaloneDatabaseProvider(context.applicationContext).also {
            databaseProvider = it
        }
    }

    fun stream(context: Context, directory: File, evictor: CacheEvictor): SimpleCache = synchronized(lock) {
        stream ?: SimpleCache(directory, evictor, database(context)).also { stream = it }
    }

    fun downloads(context: Context): SimpleCache = synchronized(lock) {
        downloads ?: SimpleCache(
            context.applicationContext.filesDir.resolve("downloads").apply { mkdirs() },
            NoOpCacheEvictor(),
            database(context)
        ).also { downloads = it }
    }
}
