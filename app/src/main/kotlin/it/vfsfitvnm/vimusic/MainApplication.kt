package it.vfsfitvnm.vimusic

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import it.vfsfitvnm.vimusic.enums.CoilDiskCacheMaxSize
import it.vfsfitvnm.vimusic.utils.applyInnertubeLocale
import it.vfsfitvnm.vimusic.utils.coilDiskCacheMaxSizeKey
import it.vfsfitvnm.vimusic.utils.getEnum
import it.vfsfitvnm.vimusic.utils.preferredAppLanguage
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.PlaybackLogStore
import it.vfsfitvnm.vimusic.utils.withAppLanguage

class MainApplication : Application(), ImageLoaderFactory {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withAppLanguage())
    }

    override fun onCreate() {
        super.onCreate()
        applyInnertubeLocale(preferredAppLanguage())
        PlaybackLogStore.init(this)
        DatabaseInitializer()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .respectCacheHeaders(false)
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes(
                        preferences.getEnum(
                            coilDiskCacheMaxSizeKey,
                            CoilDiskCacheMaxSize.`128MB`
                        ).bytes
                    )
                    .build()
            )
            .build()
    }
}
