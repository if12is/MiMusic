package it.vfsfitvnm.vimusic

import android.Manifest
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import android.os.IBinder
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.defaultShimmerTheme
import it.vfsfitvnm.compose.persist.PersistMap
import it.vfsfitvnm.compose.persist.PersistMapOwner
import it.vfsfitvnm.innertube.Innertube
import it.vfsfitvnm.innertube.models.bodies.BrowseBody
import it.vfsfitvnm.innertube.requests.playlistPage
import it.vfsfitvnm.innertube.requests.song
import it.vfsfitvnm.vimusic.enums.AppLanguage
import it.vfsfitvnm.vimusic.enums.BuiltInPlaylist
import it.vfsfitvnm.vimusic.enums.ColorPaletteMode
import it.vfsfitvnm.vimusic.enums.ColorPaletteName
import it.vfsfitvnm.vimusic.enums.NavigationStyle
import it.vfsfitvnm.vimusic.enums.ThumbnailRoundness
import it.vfsfitvnm.vimusic.service.PlayerService
import it.vfsfitvnm.vimusic.ui.components.BottomSheetMenu
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.rememberBottomSheetState
import it.vfsfitvnm.vimusic.ui.components.themed.ConfirmationDialog
import it.vfsfitvnm.vimusic.ui.components.themed.GlassNavigationHost
import it.vfsfitvnm.vimusic.ui.components.themed.LocalGlassNavigationHost
import it.vfsfitvnm.vimusic.ui.components.themed.NavigationBar
import it.vfsfitvnm.vimusic.ui.screens.albumRoute
import it.vfsfitvnm.vimusic.ui.screens.artistRoute
import it.vfsfitvnm.vimusic.ui.screens.builtInPlaylistRoute
import it.vfsfitvnm.vimusic.ui.screens.home.HomeScreen
import it.vfsfitvnm.vimusic.ui.screens.player.Player
import it.vfsfitvnm.vimusic.ui.screens.playlistRoute
import it.vfsfitvnm.vimusic.ui.screens.searchRoute
import it.vfsfitvnm.vimusic.ui.styling.Appearance
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.ui.styling.LocalAppearance
import it.vfsfitvnm.vimusic.ui.styling.UiStrings
import it.vfsfitvnm.vimusic.ui.styling.colorPaletteOf
import it.vfsfitvnm.vimusic.ui.styling.dynamicColorPaletteOf
import it.vfsfitvnm.vimusic.ui.styling.typographyOf
import it.vfsfitvnm.vimusic.utils.LocalAppLanguage
import it.vfsfitvnm.vimusic.utils.LocalStrings
import it.vfsfitvnm.vimusic.utils.appFontKey
import it.vfsfitvnm.vimusic.utils.appLanguageKey
import it.vfsfitvnm.vimusic.utils.appLockKey
import it.vfsfitvnm.vimusic.utils.applyFontPaddingKey
import it.vfsfitvnm.vimusic.utils.applyInnertubeLocale
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.colorPaletteModeKey
import it.vfsfitvnm.vimusic.utils.colorPaletteNameKey
import it.vfsfitvnm.vimusic.utils.forcePlay
import it.vfsfitvnm.vimusic.utils.getEnum
import it.vfsfitvnm.vimusic.utils.GitHubUpdater
import it.vfsfitvnm.vimusic.utils.hideFromRecentsKey
import it.vfsfitvnm.vimusic.utils.intent
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid13
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid8
import it.vfsfitvnm.vimusic.utils.keepScreenOnKey
import it.vfsfitvnm.vimusic.utils.navigationStyleKey
import it.vfsfitvnm.vimusic.utils.lastUpdateCheckMsKey
import it.vfsfitvnm.vimusic.utils.onboardingDoneKey
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.preferredAppLanguage
import it.vfsfitvnm.vimusic.utils.preferences
import it.vfsfitvnm.vimusic.utils.selectedAppFont
import it.vfsfitvnm.vimusic.utils.thumbnailRoundnessKey
import it.vfsfitvnm.vimusic.utils.useSystemFontKey
import it.vfsfitvnm.vimusic.utils.withAppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), PersistMapOwner {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlayerService.Binder) {
                this@MainActivity.binder = service
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    private var binder by mutableStateOf<PlayerService.Binder?>(null)

    override lateinit var persistMap: PersistMap

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLanguage())
    }

    override fun onStart() {
        super.onStart()
        bindService(intent<PlayerService>(), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (
            android.os.Build.VERSION.SDK_INT >= 26 &&
            preferences.getBoolean(it.vfsfitvnm.vimusic.utils.pipOnLeaveKey, false) &&
            binder?.player?.isPlaying == true
        ) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        persistMap = lastCustomNonConfigurationInstance as? PersistMap ?: PersistMap()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        requestNotificationPermission()
        maybeCheckForUpdates()

        val launchedFromNotification = intent?.extras?.getBoolean("expandPlayerBottomSheet") == true

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val isSystemInDarkTheme = isSystemInDarkTheme()

            var appearance by rememberSaveable(
                isSystemInDarkTheme,
                stateSaver = Appearance.Companion
            ) {
                with(preferences) {
                    val colorPaletteName = getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
                    val colorPaletteMode = getEnum(colorPaletteModeKey, ColorPaletteMode.System)
                    val thumbnailRoundness =
                        getEnum(thumbnailRoundnessKey, ThumbnailRoundness.Light)

                    val appLanguage = getEnum(appLanguageKey, AppLanguage.Arabic)
                    applyInnertubeLocale(appLanguage)
                    val appFont = selectedAppFont()
                    val applyFontPadding = getBoolean(applyFontPaddingKey, false)

                    val colorPalette =
                        colorPaletteOf(colorPaletteName, colorPaletteMode, isSystemInDarkTheme)

                    setSystemBarAppearance(colorPalette.isDark)

                    mutableStateOf(
                        Appearance(
                            colorPalette = colorPalette,
                            typography = typographyOf(colorPalette.text, appFont, applyFontPadding),
                            thumbnailShape = thumbnailRoundness.shape()
                        )
                    )
                }
            }

            DisposableEffect(binder, isSystemInDarkTheme) {
                var bitmapListenerJob: Job? = null

                fun setDynamicPalette(colorPaletteMode: ColorPaletteMode) {
                    val isDark =
                        colorPaletteMode == ColorPaletteMode.Dark || (colorPaletteMode == ColorPaletteMode.System && isSystemInDarkTheme)

                    binder?.setBitmapListener { bitmap: Bitmap? ->
                        if (bitmap == null) {
                            val colorPalette =
                                colorPaletteOf(
                                    ColorPaletteName.Dynamic,
                                    colorPaletteMode,
                                    isSystemInDarkTheme
                                )

                            setSystemBarAppearance(colorPalette.isDark)

                            appearance = appearance.copy(
                                colorPalette = colorPalette,
                                typography = appearance.typography.copy(colorPalette.text)
                            )

                            return@setBitmapListener
                        }

                        bitmapListenerJob = coroutineScope.launch(Dispatchers.IO) {
                            dynamicColorPaletteOf(bitmap, isDark)?.let {
                                withContext(Dispatchers.Main) {
                                    setSystemBarAppearance(it.isDark)
                                }
                                appearance = appearance.copy(
                                    colorPalette = it,
                                    typography = appearance.typography.copy(it.text)
                                )
                            }
                        }
                    }
                }

                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                        when (key) {
                            colorPaletteNameKey, colorPaletteModeKey -> {
                                val colorPaletteName =
                                    sharedPreferences.getEnum(
                                        colorPaletteNameKey,
                                        ColorPaletteName.Dynamic
                                    )

                                val colorPaletteMode =
                                    sharedPreferences.getEnum(
                                        colorPaletteModeKey,
                                        ColorPaletteMode.System
                                    )

                                if (colorPaletteName == ColorPaletteName.Dynamic) {
                                    setDynamicPalette(colorPaletteMode)
                                } else {
                                    bitmapListenerJob?.cancel()
                                    binder?.setBitmapListener(null)

                                    val colorPalette = colorPaletteOf(
                                        colorPaletteName,
                                        colorPaletteMode,
                                        isSystemInDarkTheme
                                    )

                                    setSystemBarAppearance(colorPalette.isDark)

                                    appearance = appearance.copy(
                                        colorPalette = colorPalette,
                                        typography = appearance.typography.copy(colorPalette.text),
                                    )
                                }
                            }

                            thumbnailRoundnessKey -> {
                                val thumbnailRoundness =
                                    sharedPreferences.getEnum(key, ThumbnailRoundness.Light)

                                appearance = appearance.copy(
                                    thumbnailShape = thumbnailRoundness.shape()
                                )
                            }

                            appLanguageKey -> {
                                val appLanguage = sharedPreferences.getEnum(
                                    appLanguageKey,
                                    AppLanguage.Arabic
                                )
                                applyInnertubeLocale(appLanguage)
                                recreate()
                            }

                            appFontKey, applyFontPaddingKey, useSystemFontKey -> {
                                val appFont = sharedPreferences.selectedAppFont()
                                val applyFontPadding = sharedPreferences.getBoolean(applyFontPaddingKey, false)

                                appearance = appearance.copy(
                                    typography = typographyOf(appearance.colorPalette.text, appFont, applyFontPadding),
                                )
                            }
                        }
                    }

                with(preferences) {
                    registerOnSharedPreferenceChangeListener(listener)

                    val colorPaletteName = getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
                    if (colorPaletteName == ColorPaletteName.Dynamic) {
                        setDynamicPalette(getEnum(colorPaletteModeKey, ColorPaletteMode.System))
                    }

                    onDispose {
                        bitmapListenerJob?.cancel()
                        binder?.setBitmapListener(null)
                        unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }
            }

            val rippleTheme =
                remember(appearance.colorPalette.text, appearance.colorPalette.isDark) {
                    object : RippleTheme {
                        @Composable
                        override fun defaultColor(): Color = RippleTheme.defaultRippleColor(
                            contentColor = appearance.colorPalette.text,
                            lightTheme = !appearance.colorPalette.isDark
                        )

                        @Composable
                        override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
                            contentColor = appearance.colorPalette.text,
                            lightTheme = !appearance.colorPalette.isDark
                        )
                    }
                }

            val shimmerTheme = remember {
                defaultShimmerTheme.copy(
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 800,
                            easing = LinearEasing,
                            delayMillis = 250,
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    shaderColors = listOf(
                        Color.Unspecified.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.50f),
                        Color.Unspecified.copy(alpha = 0.25f),
                    ),
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appearance.colorPalette.background0)
            ) {
                val density = LocalDensity.current
                val windowsInsets = WindowInsets.systemBars
                val bottomDp = with(density) { windowsInsets.getBottom(density).toDp() }
                val navigationStyle by rememberPreference(
                    navigationStyleKey,
                    NavigationStyle.Side
                )
                val isGlassNavigation = navigationStyle == NavigationStyle.GlassBottom
                val glassNavigationHost = remember { GlassNavigationHost() }
                val glassIdleSpace =
                    if (isGlassNavigation) Dimensions.glassNavigationBarSpace else 0.dp
                val glassDockedSpace =
                    if (isGlassNavigation) Dimensions.glassNavigationDockedHeight else 0.dp
                val collapsedBound = Dimensions.collapsedPlayer + if (isGlassNavigation) {
                    glassDockedSpace + bottomDp
                } else {
                    bottomDp
                }

                val playerBottomSheetState = rememberBottomSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = collapsedBound,
                    expandedBound = maxHeight,
                )

                val playerAwareWindowInsets by remember(
                    bottomDp,
                    glassIdleSpace,
                    glassDockedSpace,
                    isGlassNavigation,
                    playerBottomSheetState.value
                ) {
                    derivedStateOf {
                        val bottom = if (isGlassNavigation) {
                            if (playerBottomSheetState.value <= 0.dp) {
                                bottomDp + glassIdleSpace
                            } else {
                                Dimensions.collapsedPlayer + glassDockedSpace + bottomDp
                            }
                        } else {
                            bottomDp + playerBottomSheetState.value.coerceAtMost(
                                Dimensions.collapsedPlayer
                            )
                        }

                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(bottom = bottom))
                    }
                }

                val appLanguage = remember {
                    preferences.getEnum(appLanguageKey, AppLanguage.Arabic)
                }
                val keepScreenOn by rememberPreference(keepScreenOnKey, false)
                DisposableEffect(keepScreenOn) {
                    if (keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    onDispose {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                val hideFromRecents by rememberPreference(hideFromRecentsKey, false)
                DisposableEffect(hideFromRecents) {
                    if (hideFromRecents) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                    onDispose {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                var appLock by rememberPreference(appLockKey, false)
                var unlocked by rememberSaveable { mutableStateOf(false) }
                val credentialLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    unlocked = result.resultCode == android.app.Activity.RESULT_OK
                }
                LaunchedEffect(appLock) {
                    if (!appLock) {
                        unlocked = true
                        return@LaunchedEffect
                    }
                    if (unlocked) return@LaunchedEffect
                    val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    val credentialIntent = keyguard.createConfirmDeviceCredentialIntent(
                        UiStrings(preferredAppLanguage()).appLock,
                        UiStrings(preferredAppLanguage()).appLockDescription
                    )
                    if (credentialIntent != null) {
                        credentialLauncher.launch(credentialIntent)
                    } else {
                        unlocked = true
                    }
                }

                var onboardingDone by rememberPreference(onboardingDoneKey, false)

                CompositionLocalProvider(
                    LocalAppearance provides appearance,
                    LocalIndication provides rememberRipple(bounded = true),
                    LocalRippleTheme provides rippleTheme,
                    LocalShimmerTheme provides shimmerTheme,
                    LocalPlayerServiceBinder provides binder,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalAppLanguage provides appLanguage,
                    LocalStrings provides UiStrings(appLanguage),
                    LocalLayoutDirection provides if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    LocalGlassNavigationHost provides glassNavigationHost
                ) {
                    HomeScreen(
                        onPlaylistUrl = { url ->
                            onNewIntent(Intent.parseUri(url, 0))
                        }
                    )

                    Player(
                        layoutState = playerBottomSheetState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    if (isGlassNavigation &&
                        glassNavigationHost.isActive &&
                        !playerBottomSheetState.isExpanded
                    ) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            glassNavigationHost.NavigationBar(
                                docked = !playerBottomSheetState.isDismissed
                            )
                        }
                    }

                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    )

                    if (appLock && !unlocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(appearance.colorPalette.background0)
                                .clickable(enabled = false) {}
                        )
                    }

                    if (!onboardingDone) {
                        ConfirmationDialog(
                            text = "${LocalStrings.current.onboardingTitle}\n\n${LocalStrings.current.onboardingBody}",
                            confirmText = LocalStrings.current.gotIt,
                            cancelText = LocalStrings.current.gotIt,
                            onConfirm = { onboardingDone = true },
                            onDismiss = { onboardingDone = true }
                        )
                    }
                }

                DisposableEffect(binder?.player) {
                    val player = binder?.player ?: return@DisposableEffect onDispose { }

                    if (player.currentMediaItem == null) {
                        if (!playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.dismiss()
                        }
                    } else {
                        if (playerBottomSheetState.isDismissed) {
                            if (launchedFromNotification) {
                                intent.replaceExtras(Bundle())
                                playerBottomSheetState.expand(tween(700))
                            } else {
                                playerBottomSheetState.collapse(tween(700))
                            }
                        }
                    }

                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null) {
                                if (mediaItem.mediaMetadata.extras?.getBoolean("isFromPersistentQueue") != true) {
                                    playerBottomSheetState.expand(tween(500))
                                } else {
                                    playerBottomSheetState.collapse(tween(700))
                                }
                            }
                        }
                    }

                    player.addListener(listener)

                    onDispose { player.removeListener(listener) }
                }
            }
        }

        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        when (intent.action) {
            "it.vfsfitvnm.vimusic.SEARCH" -> {
                this.intent = null
                lifecycleScope.launch {
                    searchRoute.ensureGlobal("")
                }
                return
            }

            "it.vfsfitvnm.vimusic.FAVORITES" -> {
                this.intent = null
                lifecycleScope.launch {
                    builtInPlaylistRoute.ensureGlobal(BuiltInPlaylist.Favorites)
                }
                return
            }

            "it.vfsfitvnm.vimusic.OFFLINE" -> {
                this.intent = null
                lifecycleScope.launch {
                    builtInPlaylistRoute.ensureGlobal(BuiltInPlaylist.Offline)
                }
                return
            }
        }

        val uri = intent.data ?: return

        intent.data = null
        this.intent = null

        Toast.makeText(this, UiStrings(preferredAppLanguage()).openingUrl, Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            when (val path = uri.pathSegments.firstOrNull()) {
                "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                    val browseId = "VL$playlistId"

                    if (playlistId.startsWith("OLAK5uy_")) {
                        Innertube.playlistPage(BrowseBody(browseId = browseId))?.getOrNull()?.let {
                            it.songsPage?.items?.firstOrNull()?.album?.endpoint?.browseId?.let { browseId ->
                                albumRoute.ensureGlobal(browseId)
                            }
                        }
                    } else {
                        playlistRoute.ensureGlobal(browseId)
                    }
                }

                "channel", "c" -> uri.lastPathSegment?.let { channelId ->
                    artistRoute.ensureGlobal(channelId)
                }

                else -> when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> path
                    else -> null
                }?.let { videoId ->
                    Innertube.song(videoId)?.getOrNull()?.let { song ->
                        val binder = snapshotFlow { binder }.filterNotNull().first()
                        withContext(Dispatchers.Main) {
                            binder.player.forcePlay(song.asMediaItem)
                        }
                    }
                }
            }
        }
    }

    override fun onRetainCustomNonConfigurationInstance() = persistMap

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            persistMap.clear()
        }

        super.onDestroy()
    }

    private fun requestNotificationPermission() {
        if (
            isAtLeastAndroid13 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private fun maybeCheckForUpdates() {
        lifecycleScope.launch(Dispatchers.IO) {
            val lastCheckMs = preferences.getLong(lastUpdateCheckMsKey, 0L)
            if (System.currentTimeMillis() - lastCheckMs < 24 * 60 * 60 * 1000L) {
                return@launch
            }

            preferences.edit { putLong(lastUpdateCheckMsKey, System.currentTimeMillis()) }

            val release = runCatching { GitHubUpdater.fetchLatestRelease() }.getOrNull()
                ?: return@launch
            if (!release.isNewer) return@launch

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    UiStrings(preferredAppLanguage()).updateAvailableText(release.versionName),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setSystemBarAppearance(isDark: Boolean) {
        with(WindowCompat.getInsetsController(window, window.decorView.rootView)) {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        if (!isAtLeastAndroid6) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }

        if (!isAtLeastAndroid8) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }
}

val LocalPlayerServiceBinder = staticCompositionLocalOf<PlayerService.Binder?> { null }

val LocalPlayerAwareWindowInsets = staticCompositionLocalOf<WindowInsets> { TODO() }
