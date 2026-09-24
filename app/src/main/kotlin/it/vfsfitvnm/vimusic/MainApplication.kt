package it.vfsfitvnm.vimusic

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import it.vfsfitvnm.vimusic.enums.CoilDiskCacheMaxSize
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.vimusic.utils.PlaybackLogStore
import it.vfsfitvnm.vimusic.utils.Region
import it.vfsfitvnm.vimusic.utils.SecretStore
import it.vfsfitvnm.vimusic.utils.youtubeCookieKey
import it.vfsfitvnm.vimusic.utils.applyInnertubeLocale
import it.vfsfitvnm.vimusic.utils.coilDiskCacheMaxSizeKey
import it.vfsfitvnm.vimusic.utils.getEnum
import it.vfsfitvnm.vimusic.utils.preferredAppLanguage
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.withAppLanguage
import org.acra.ACRA
import org.acra.config.CoreConfigurationBuilder
import org.acra.data.StringFormat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainApplication : Application(), ImageLoaderFactory {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withAppLanguage())
        PlaybackLogStore.init(this)
        ACRA.init(
            this,
            CoreConfigurationBuilder()
                .withBuildConfigClass(BuildConfig::class.java)
                .withReportFormat(StringFormat.JSON)
                .withAlsoReportToAndroidFramework(true)
        )
    }

    override fun onCreate() {
        super.onCreate()
        SecretStore.init(this)
        Innertube.cookie = SecretStore.get(youtubeCookieKey).ifBlank { null }
        Region.load(this)
        applyInnertubeLocale(preferredAppLanguage())
        MainScope().launch {
            Region.refresh(this@MainApplication)
            applyInnertubeLocale(preferredAppLanguage())
        }
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
