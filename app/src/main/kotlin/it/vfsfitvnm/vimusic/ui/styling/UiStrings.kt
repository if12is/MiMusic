package it.vfsfitvnm.vimusic.ui.styling

import it.vfsfitvnm.vimusic.enums.AlbumSortBy
import it.vfsfitvnm.vimusic.enums.AppFont
import it.vfsfitvnm.vimusic.enums.AppLanguage
import it.vfsfitvnm.vimusic.enums.ArtistSortBy
import it.vfsfitvnm.vimusic.enums.ColorPaletteMode
import it.vfsfitvnm.vimusic.enums.ColorPaletteName
import it.vfsfitvnm.vimusic.enums.PlaylistSortBy
import it.vfsfitvnm.vimusic.enums.SongSortBy
import it.vfsfitvnm.vimusic.enums.NavigationStyle
import it.vfsfitvnm.vimusic.enums.ThumbnailRoundness

class UiStrings(private val language: AppLanguage) {
    private val ar: Boolean
        get() = language == AppLanguage.Arabic

    private fun t(english: String, arabic: String) = if (ar) arabic else english

    val languageTitle = t("Language", "اللغة")
    val languageDescription = t("Arabic is the default interface language", "العربية هي لغة الواجهة الأساسية")
    val contentRegion = t("Content region", "منطقة المحتوى")
    val contentRegionAuto = t("Automatic", "تلقائي")
    val contentRegionDescription = t(
        "Automatic uses your IP address only to choose the country for trends. You can pick a country instead.",
        "الوضع التلقائي يستخدم عنوان IP لمعرفة الدولة للترند فقط. تقدر تختار الدولة بنفسك."
    )

    val quickPicks = t("Quick picks", "اختيارات سريعة")
    val songs = t("Songs", "الأغاني")
    val playlists = t("Playlists", "قوائم التشغيل")
    val artists = t("Artists", "الفنانون")
    val albums = t("Albums", "الألبومات")
    val videos = t("Videos", "الفيديوهات")
    val featured = t("Featured", "مميزة")
    val singles = t("Singles", "الأغاني المنفردة")
    val overview = t("Overview", "نظرة عامة")
    val library = t("Library", "المكتبة")
    val online = t("Online", "عبر الإنترنت")
    val otherVersions = t("Other versions", "نسخ أخرى")
    val relatedAlbums = t("Related albums", "ألبومات ذات صلة")
    val similarArtists = t("Similar artists", "فنانون مشابهون")
    val playlistsYouMightLike = t("Playlists you might like", "قوائم قد تعجبك")
    val favorites = t("Favorites", "المفضلة")
    val offline = t("Offline", "بدون اتصال")

    val appearance = t("Appearance", "المظهر")
    val player = t("Player", "المشغّل")
    val playerAndAudio = t("Player & Audio", "المشغّل والصوت")
    val cache = t("Cache", "التخزين المؤقت")
    val database = t("Database", "قاعدة البيانات")
    val other = t("Other", "أخرى")
    val about = t("About", "حول")

    val colors = t("COLORS", "الألوان")
    val shapes = t("SHAPES", "الأشكال")
    val navigationGroup = t("NAVIGATION", "التنقل")
    val navigationStyle = t("Menu style", "شكل القائمة")
    val navigationStyleDescription = t(
        "Keep the side menu or use a glassy bar at the bottom",
        "أبقِ القائمة الجانبية أو استخدم شريطاً زجاجياً في الأسفل"
    )
    val navigationStyleSide = t("Side menu", "قائمة جانبية")
    val navigationStyleGlass = t("Glassy bottom bar", "شريط سفلي زجاجي")
    val navigationAction = t("Menu action", "إجراء القائمة")
    val navigationBack = t("Back", "رجوع")
    val speedShort = t("Speed", "السرعة")
    val sleepTimerShort = t("Timer", "المؤقت")
    val moreTools = t("More", "المزيد")
    val repeatSong = t("Repeat song", "تكرار الأغنية")
    val modeAudio = t("Song", "صوت")
    val modeVideo = t("Video", "فيديو")
    val videoAvailableHint = t("A video is available for this track", "هذا المقطع متاح كفيديو أيضاً")
    val videoModeSetting = t("Play music videos", "تشغيل الفيديو بدل الصوت")
    val videoModeSettingDescription = t(
        "Off plays audio only (saves data). You can switch per track from the player.",
        "مغلق = صوت فقط (يوفّر البيانات). يمكنك التبديل لكل مقطع من المشغّل."
    )
    fun trendingIn(country: String) = t("Trending in $country", "الترند في $country")
    fun mostPopularIn(country: String) = t("Most popular in $country", "الأكثر انتشاراً في $country")
    val newArabicSongs = t("New Arabic releases", "جديد الأغاني العربية")
    val topArabicSongs = t("Most played Arabic songs", "الأعلى استماعاً عربياً")
    val classicArabicSongs = t("Classic Arabic & tarab", "طرب وتراث أصيل")
    val newReleases = t("New releases", "إصدارات جديدة")
    val navigationSettings = t("Settings", "الإعدادات")
    val textGroup = t("TEXT", "النص")
    val lockscreen = t("LOCKSCREEN", "شاشة القفل")
    val playerGroup = t("PLAYER", "المشغّل")
    val audioGroup = t("AUDIO", "الصوت")
    val languageGroup = t("LANGUAGE", "اللغة")

    val theme = t("Theme", "السمة")
    val themeMode = t("Theme mode", "وضع السمة")
    val thumbnailRoundness = t("Thumbnail roundness", "استدارة الصورة")
    val useSystemFont = t("Use system font", "استخدام خط النظام")
    val useSystemFontDescription = t(
        "Use the system font instead of Cairo for Arabic and English names",
        "استخدم خط النظام بدلاً من خط Cairo لأسماء الأغاني العربية والإنجليزية"
    )
    val appFont = t("Font", "الخط")
    val appFontDescription = t(
        "Choose a typeface for Arabic and English text",
        "اختر نوع الخط للنصوص العربية والإنجليزية"
    )
    val appFontCairo = t("Cairo", "Cairo")
    val appFontTajawal = t("Tajawal", "Tajawal")
    val appFontAmiri = t("Amiri", "Amiri")
    val appFontPoppins = t("Poppins", "Poppins")
    val appFontSystem = t("System", "خط النظام")
    val applyFontPadding = t("Apply font padding", "تطبيق تباعد الخط")
    val applyFontPaddingDescription = t("Add spacing around texts", "إضافة مسافات حول النصوص")
    val showSongCover = t("Show song cover", "إظهار غلاف الأغنية")
    val showSongCoverDescription = t(
        "Use the playing song cover as the lockscreen wallpaper",
        "استخدم غلاف الأغنية الحالية كخلفية لشاشة القفل"
    )

    val themeDefault = t("Default", "افتراضي")
    val themeDynamic = t("Dynamic", "ديناميكي")
    val themePureBlack = t("Pure black", "أسود خالص")
    val themeLight = t("Light", "فاتح")
    val themeDark = t("Dark", "داكن")
    val themeSystem = t("System", "النظام")
    val roundnessNone = t("None", "بدون")
    val roundnessLight = t("Light", "خفيف")
    val roundnessMedium = t("Medium", "متوسط")
    val roundnessHeavy = t("Heavy", "ثقيل")

    val persistentQueue = t("Persistent queue", "قائمة تشغيل دائمة")
    val persistentQueueDescription = t("Save and restore playing songs", "حفظ الأغاني قيد التشغيل واستعادتها")
    val resumePlayback = t("Resume playback", "استئناف التشغيل")
    val resumePlaybackDescription = t(
        "When a wired or bluetooth device is connected",
        "عند توصيل سماعة سلكية أو بلوتوث"
    )
    val skipSilence = t("Skip silence", "تخطي الصمت")
    val skipSilenceDescription = t("Skip silent parts during playback", "تخطي الأجزاء الصامتة أثناء التشغيل")
    val loudnessNormalization = t("Loudness normalization", "توحيد مستوى الصوت")
    val loudnessNormalizationDescription = t("Adjust the volume to a fixed level", "ضبط الصوت على مستوى ثابت")
    val equalizer = t("Equalizer", "المعادل")
    val equalizerDescription = t("Interact with the system equalizer", "فتح معادل الصوت في النظام")
    val equalizerMissing = t("Couldn't find an application to equalize audio", "تعذر العثور على تطبيق لمعادلة الصوت")

    val imageCache = t("IMAGE CACHE", "ذاكرة الصور")
    val songCache = t("SONG CACHE", "ذاكرة الأغاني")
    val maxSize = t("Max size", "الحد الأقصى للحجم")
    val cacheDescription = t(
        "When the cache runs out of space, the resources that haven't been accessed for the longest time are cleared",
        "عند امتلاء الذاكرة المؤقتة يتم حذف الموارد الأقدم استخداماً"
    )

    val cleanup = t("CLEANUP", "تنظيف")
    val backup = t("BACKUP", "نسخ احتياطي")
    val restore = t("RESTORE", "استعادة")
    val resetQuickPicks = t("Reset quick picks", "إعادة تعيين الاختيارات السريعة")
    val quickPicksCleared = t("Quick picks are cleared", "تم مسح الاختيارات السريعة")
    val backupTitle = t("Backup", "نسخ احتياطي")
    val backupDescription = t("Export the database to the external storage", "تصدير قاعدة البيانات إلى وحدة التخزين")
    val backupPreferencesNote = t(
        "Personal preferences (i.e. the theme mode) and the cache are excluded.",
        "يتم استبعاد التفضيلات الشخصية (مثل وضع السمة) والذاكرة المؤقتة."
    )
    val restoreTitle = t("Restore", "استعادة")
    val restoreDescription = t("Import the database from the external storage", "استيراد قاعدة البيانات من وحدة التخزين")
    val packageMoveDescription = t(
        "This version is a separate app from the old ViMusic package. Export the database here before removing the old install, then import it in this one.",
        "النسخة دي تطبيق منفصل عن حزمة ViMusic القديمة. صدّر قاعدة البيانات من هنا قبل حذف النسخة القديمة، ثم استوردها في النسخة الجديدة."
    )
    fun restoreOverwriteWarning(appName: String) = t(
        "Existing data will be overwritten.\n$appName will automatically close itself after restoring the database.",
        "سيتم استبدال البيانات الحالية.\nسيتم إغلاق $appName تلقائياً بعد استعادة قاعدة البيانات."
    )
    val documentsCreateMissing = t("Couldn't find an application to create documents", "تعذر العثور على تطبيق لإنشاء المستندات")
    val documentsOpenMissing = t("Couldn't find an application to open documents", "تعذر العثور على تطبيق لفتح المستندات")
    fun deletePlaybackEvents(count: Int) = t(
        "Delete $count playback events",
        "حذف $count من أحداث التشغيل"
    )

    val androidAuto = t("ANDROID AUTO", "أندرويد أوتو")
    val androidAutoTitle = t("Android Auto", "أندرويد أوتو")
    val androidAutoDescription = t("Enable Android Auto support", "تفعيل دعم أندرويد أوتو")
    val androidAutoHint = t(
        "Remember to enable \"Unknown sources\" in the Developer Settings of Android Auto.",
        "تذكّر تفعيل \"مصادر غير معروفة\" في إعدادات المطوّر لأندرويد أوتو."
    )
    val searchHistory = t("SEARCH HISTORY", "سجل البحث")
    val pauseSearchHistory = t("Pause search history", "إيقاف سجل البحث")
    val pauseSearchHistoryDescription = t(
        "Neither save new searched queries nor show history",
        "عدم حفظ عمليات البحث الجديدة أو عرض السجل"
    )
    val clearSearchHistory = t("Clear search history", "مسح سجل البحث")
    fun deleteSearchQueries(count: Int) = t("Delete $count search queries", "حذف $count من عمليات البحث")
    val historyEmpty = t("History is empty", "السجل فارغ")
    val serviceLifetime = t("SERVICE LIFETIME", "عمر الخدمة")
    val batteryOptimizationWarning = t(
        "If battery optimizations are applied, the playback notification can suddenly disappear when paused.",
        "إذا كانت تحسينات البطارية مفعّلة فقد يختفي إشعار التشغيل فجأة عند الإيقاف المؤقت."
    )
    val batteryOptimizationAndroid12 = t(
        "Since Android 12, disabling battery optimizations is required for the \"Invincible service\" option to take effect.",
        "منذ أندرويد 12 يجب تعطيل تحسينات البطارية حتى يعمل خيار \"الخدمة الدائمة\"."
    )
    val ignoreBatteryOptimizations = t("Ignore battery optimizations", "تجاهل تحسينات البطارية")
    val alreadyUnrestricted = t("Already unrestricted", "غير مقيّد بالفعل")
    val disableBackgroundRestrictions = t("Disable background restrictions", "تعطيل قيود الخلفية")
    val batterySettingsMissing = t(
        "Couldn't find battery optimization settings, please whitelist MiMusic manually",
        "تعذر العثور على إعدادات تحسين البطارية، يرجى السماح لتطبيق MiMusic يدوياً"
    )
    val invincibleService = t("Invincible service", "الخدمة الدائمة")
    val invincibleServiceDescription = t(
        "When turning off battery optimizations is not enough",
        "عندما لا يكفي تعطيل تحسينات البطارية"
    )

    val social = t("SOCIAL", "التواصل")
    val troubleshooting = t("TROUBLESHOOTING", "استكشاف الأخطاء")
    val github = t("GitHub", "GitHub")
    val viewSource = t("View the source code", "عرض الشفرة المصدرية")
    val reportIssue = t("Report an issue", "الإبلاغ عن مشكلة")
    val requestFeature = t("Request a feature or suggest an idea", "اقتراح ميزة أو فكرة")
    val redirectedToGithub = t("You will be redirected to GitHub", "سيتم توجيهك إلى GitHub")

    val unknown = t("Unknown", "غير معروف")
    val enqueue = t("Enqueue", "إضافة إلى القائمة")
    val newPlaylist = t("New playlist", "قائمة تشغيل جديدة")
    val enterPlaylistName = t("Enter the playlist name", "أدخل اسم قائمة التشغيل")
    val enterAName = t("Enter a name", "أدخل اسماً")
    val enterLyrics = t("Enter the lyrics", "أدخل كلمات الأغنية")
    val clear = t("Clear", "مسح")
    val cancel = t("Cancel", "إلغاء")
    val done = t("Done", "تم")
    val confirm = t("Confirm", "تأكيد")
    val set = t("Set", "تعيين")
    val stop = t("Stop", "إيقاف")
    val sync = t("Sync", "مزامنة")
    val rename = t("Rename", "إعادة تسمية")
    val delete = t("Delete", "حذف")
    val hide = t("Hide", "إخفاء")
    val shuffle = t("Shuffle", "خلط")
    val playAll = t("Play all", "تشغيل الكل")
    val viewAll = t("View all", "عرض الكل")
    val startRadio = t("Start radio", "بدء الراديو")
    val playNext = t("Play next", "تشغيل التالي")
    val addToPlaylist = t("Add to playlist", "إضافة إلى قائمة تشغيل")
    val goToAlbum = t("Go to album", "الذهاب إلى الألبوم")
    val removeFromQueue = t("Remove from queue", "إزالة من قائمة الانتظار")
    val removeFromPlaylist = t("Remove from playlist", "إزالة من قائمة التشغيل")
    val hideFromQuickPicks = t("Hide from \"Quick picks\"", "إخفاء من \"اختيارات سريعة\"")
    val sleepTimer = t("Sleep timer", "مؤقت النوم")
    val setSleepTimer = t("Set sleep timer", "تعيين مؤقت النوم")
    val stopSleepTimer = t("Do you want to stop the sleep timer?", "هل تريد إيقاف مؤقت النوم؟")
    val sleepTimerEnded = t("Sleep timer ended", "انتهى مؤقت النوم")
    val nowPlaying = t("Now playing", "قيد التشغيل الآن")
    val queueLoop = t("Queue loop", "تكرار القائمة")
    val lyrics = t("Lyrics", "الكلمات")
    val editLyrics = t("Edit lyrics", "تعديل الكلمات")
    val searchLyricsOnline = t("Search lyrics online", "البحث عن الكلمات عبر الإنترنت")
    val fetchLyricsAgain = t("Fetch lyrics again", "جلب الكلمات مرة أخرى")
    val providedByKugou = t("Provided by kugou.com", "مقدمة من kugou.com")
    val browseInternetMissing = t("Couldn't find an application to browse the Internet", "تعذر العثور على تطبيق لتصفح الإنترنت")
    val wikipediaAttribution = t(
        "From Wikipedia under Creative Commons Attribution CC-BY-SA 3.0",
        "من ويكيبيديا بموجب رخصة المشاع الإبداعي CC-BY-SA 3.0"
    )
    val noResults = t(
        "No results found. Please try a different query or category",
        "لا توجد نتائج. جرّب عبارة بحث أو تصنيفاً مختلفاً"
    )
    val noItemsFound = t("No items found", "لا توجد عناصر")
    val anErrorOccurred = t("An error has occurred", "حدث خطأ")
    val anErrorOccurredDot = t("An error has occurred.", "حدث خطأ.")
    val couldNotLoadHome = t(
        "Couldn't load recommendations. Check your connection and try again.",
        "تعذر تحميل التوصيات. تحقق من الاتصال ثم أعد المحاولة."
    )
    val retry = t("Retry", "إعادة المحاولة")
    val openingUrl = t("Opening url...", "جاري فتح الرابط...")
    val deletePlaylistConfirm = t("Do you really want to delete this playlist?", "هل تريد حقاً حذف قائمة التشغيل هذه؟")
    val hideSongConfirm = t(
        "Do you really want to hide this song? Its playback time and cache will be wiped.\nThis action is irreversible.",
        "هل تريد حقاً إخفاء هذه الأغنية؟ سيتم مسح وقت تشغيلها والذاكرة المؤقتة الخاصة بها.\nلا يمكن التراجع عن هذا الإجراء."
    )
    val albumHasNoAlternative = t(
        "This album doesn't have any alternative version",
        "لا يحتوي هذا الألبوم على أي نسخة بديلة"
    )
    val artistHasNoAlbum = t("This artist didn't release any album", "لم يصدر هذا الفنان أي ألبوم")
    val artistHasNoSingle = t("This artist didn't release any single", "لم يصدر هذا الفنان أي أغنية منفردة")
    val cacheDescriptionUsed = t("Used", "مستخدم")

    fun viewAlbumOrPlaylist(isAlbum: Boolean) = if (isAlbum) {
        t("View album", "عرض الألبوم")
    } else {
        t("View playlist", "عرض قائمة التشغيل")
    }

    fun moreFrom(authorName: String) = t("More from $authorName", "المزيد من $authorName")
    fun songsCount(count: Int) = t("$count songs", "$count أغنية")
    fun timeLeft(value: String) = t("$value left", "متبقي $value")

    val showSynchronizedLyrics = t("Show synchronized lyrics", "عرض كلمات متزامنة")
    val showUnsynchronizedLyrics = t("Show unsynchronized lyrics", "عرض كلمات غير متزامنة")
    val lyricsFetchError = t("An error has occurred while fetching the lyrics", "حدث خطأ أثناء جلب كلمات الأغنية")
    val synchronizedLyricsFetchError = t(
        "An error has occurred while fetching the synchronized lyrics",
        "حدث خطأ أثناء جلب الكلمات المتزامنة"
    )
    val lyricsUnavailable = t("Lyrics are not available for this song", "الكلمات غير متوفرة لهذه الأغنية")
    val synchronizedLyricsUnavailable = t(
        "Synchronized lyrics are not available for this song",
        "الكلمات المتزامنة غير متوفرة لهذه الأغنية"
    )

    val networkError = t("A network error has occurred", "حدث خطأ في الشبكة")
    val playableFormatNotFound = t("Couldn't find a playable audio format", "تعذر العثور على صيغة صوت قابلة للتشغيل")
    val unplayable = t("The original video source of this song has been deleted", "تم حذف المصدر الأصلي لهذه الأغنية")
    val loginRequired = t("This song cannot be played due to server restrictions", "لا يمكن تشغيل هذه الأغنية بسبب قيود الخادم")
    val videoIdMismatch = t("The returned video id doesn't match the requested one", "معرف الفيديو المُعاد لا يطابق المطلوب")
    val unknownPlaybackError = t("An unknown playback error has occurred", "حدث خطأ تشغيل غير معروف")

    val statsId = t("Id", "المعرف")
    val statsItag = t("Itag", "Itag")
    val statsBitrate = t("Bitrate", "معدل البت")
    val statsSize = t("Size", "الحجم")
    val statsCached = t("Cached", "مخزّن")
    val statsLoudness = t("Loudness", "مستوى الصوت")

    val skipBack = t("Skip back", "السابق")
    val skipForward = t("Skip forward", "التالي")
    val play = t("Play", "تشغيل")
    val pause = t("Pause", "إيقاف مؤقت")

    val download = t("Download", "تنزيل")
    val downloading = t("Downloading…", "جاري التنزيل…")
    val downloaded = t("Downloaded", "تم التنزيل")
    val removeDownload = t("Remove download", "إزالة التنزيل")
    val downloadStarted = t("Download started", "بدأ التنزيل")
    val downloadCompleted = t("Song downloaded for offline playback", "تم تنزيل الأغنية للتشغيل دون اتصال")
    val downloadFailed = t("Couldn't download this song", "تعذر تنزيل هذه الأغنية")
    val downloadRemoved = t("Download removed", "تمت إزالة التنزيل")

    val updates = t("UPDATES", "التحديثات")
    val checkForUpdates = t("Check for updates", "التحقق من التحديثات")
    val checkForUpdatesDescription = t(
        "Download the latest GitHub release when a newer version is available",
        "تنزيل أحدث إصدار من GitHub عند توفر نسخة أحدث"
    )
    val updateAvailable = t("Update available", "يتوفر تحديث")
    fun updateAvailableText(version: String) = t(
        "Version $version is available. Download and install it now?",
        "الإصدار $version متاح. هل تريد تنزيله وتثبيته الآن؟"
    )
    val upToDate = t("You are using the latest version", "أنت تستخدم أحدث إصدار")
    val updateCheckFailed = t("Couldn't check for updates", "تعذر التحقق من التحديثات")
    val downloadingUpdate = t("Downloading update…", "جاري تنزيل التحديث…")
    val installUpdate = t("Update", "تحديث")
    val updateSignatureMismatch = t(
        "This APK is signed differently from the installed app, so Android cannot upgrade it. Uninstall MiMusic once, then install this file.",
        "توقيع هذا الملف يختلف عن التطبيق المثبت، لذلك لا يمكن لأندرويد ترقيته. أزل MiMusic مرة واحدة ثم ثبّت هذا الملف."
    )
    val updatePackageMismatch = t(
        "This APK belongs to a different app package and cannot update the current install.",
        "هذا الملف لتطبيق مختلف ولا يمكنه تحديث النسخة الحالية."
    )
    val updateInstallFailed = t("Couldn't start the update installer", "تعذر بدء مثبت التحديث")
    val playbackLog = t("Playback log", "سجل التشغيل")
    val playbackLogDescription = t(
        "Copy the last stream-resolution attempts to find why a song will not play",
        "انسخ آخر محاولات جلب الصوت لمعرفة سبب توقف التشغيل"
    )
    val playbackLogEmpty = t("No playback log yet. Play a song first.", "لا يوجد سجل بعد. شغّل أغنية أولاً.")
    val playbackLogCopied = t("Playback log copied", "تم نسخ سجل التشغيل")

    fun colorPaletteName(value: ColorPaletteName) = when (value) {
        ColorPaletteName.Default -> themeDefault
        ColorPaletteName.Dynamic -> themeDynamic
        ColorPaletteName.PureBlack -> themePureBlack
        ColorPaletteName.Gold -> goldTheme
        ColorPaletteName.Spotify -> themeSpotify
        ColorPaletteName.YouTube -> themeYouTube
    }

    fun colorPaletteMode(value: ColorPaletteMode) = when (value) {
        ColorPaletteMode.Light -> themeLight
        ColorPaletteMode.Dark -> themeDark
        ColorPaletteMode.System -> themeSystem
    }

    fun navigationStyleName(value: NavigationStyle) = when (value) {
        NavigationStyle.Side -> navigationStyleSide
        NavigationStyle.GlassBottom -> navigationStyleGlass
    }

    fun appFontName(value: AppFont) = when (value) {
        AppFont.Cairo -> appFontCairo
        AppFont.Tajawal -> appFontTajawal
        AppFont.Amiri -> appFontAmiri
        AppFont.Poppins -> appFontPoppins
        AppFont.System -> appFontSystem
    }

    fun thumbnailRoundnessName(value: ThumbnailRoundness) = when (value) {
        ThumbnailRoundness.None -> roundnessNone
        ThumbnailRoundness.Light -> roundnessLight
        ThumbnailRoundness.Medium -> roundnessMedium
        ThumbnailRoundness.Heavy -> roundnessHeavy
    }

    fun songSortBy(value: SongSortBy) = when (value) {
        SongSortBy.PlayTime -> t("Play time", "وقت التشغيل")
        SongSortBy.Title -> t("Title", "العنوان")
        SongSortBy.DateAdded -> t("Date added", "تاريخ الإضافة")
    }

    fun albumSortBy(value: AlbumSortBy) = when (value) {
        AlbumSortBy.Title -> t("Title", "العنوان")
        AlbumSortBy.Year -> t("Year", "السنة")
        AlbumSortBy.DateAdded -> t("Date added", "تاريخ الإضافة")
    }

    fun artistSortBy(value: ArtistSortBy) = when (value) {
        ArtistSortBy.Name -> t("Name", "الاسم")
        ArtistSortBy.DateAdded -> t("Date added", "تاريخ الإضافة")
    }

    fun playlistSortBy(value: PlaylistSortBy) = when (value) {
        PlaylistSortBy.Name -> t("Name", "الاسم")
        PlaylistSortBy.DateAdded -> t("Date added", "تاريخ الإضافة")
        PlaylistSortBy.SongCount -> t("Song count", "عدد الأغاني")
    }

    val no = t("No", "لا")
    val downloadAll = t("Download all", "تنزيل الكل")
    val downloadStartedCount: (Int) -> String = { count ->
        t("Downloading $count songs", "جاري تنزيل $count أغنية")
    }
    val downloadFavorites = t("Download favorites", "تنزيل المفضلة")
    val downloadsCache = t("DOWNLOADS", "التنزيلات")
    val downloadsDescription = t(
        "Songs saved for offline playback. These are kept separately from the streaming cache.",
        "الأغاني المحفوظة للتشغيل بدون اتصال. تُحفظ منفصلة عن ذاكرة البث."
    )
    val clearDownloads = t("Remove all downloads", "حذف كل التنزيلات")
    val downloadsCleared = t("Downloads removed", "تم حذف التنزيلات")
    val playbackSpeed = t("Playback speed", "سرعة التشغيل")
    val keepScreenOn = t("Keep screen on", "إبقاء الشاشة مضاءة")
    val keepScreenOnDescription = t(
        "Prevent the screen from turning off while music is playing",
        "منع إطفاء الشاشة أثناء تشغيل الموسيقى"
    )
    val skipBackward = t("Back 15 seconds", "رجوع 15 ثانية")
    val skip15Forward = t("Forward 15 seconds", "تقديم 15 ثانية")
    val recentlyPlayed = t("Recently played", "استُمع إليه مؤخراً")
    val songOfTheDay = t("Song of the day", "أغنية اليوم")
    val moods = t("Moods", "المزاج")
    val moodCalm = t("Calm", "هادي")
    val moodEnergetic = t("Energetic", "حماسي")
    val moodTarab = t("Tarab", "طرب")
    val moodShaabi = t("Shaabi", "شعبي")
    val moodQuran = t("Quran", "قرآن")
    val moodFocus = t("Focus", "عمل")
    val moodLocal = t("Local hits", "الأشهر محليًا")
    val moodParty = t("Party", "حفلات")
    val librarySongsHint = t(
        "Songs you have played appear here",
        "الأغاني التي شغّلتها تظهر هنا"
    )
    val libraryArtistsHint = t(
        "Bookmark artists to see them here",
        "احفظ الفنانين لتظهر هنا"
    )
    val libraryAlbumsHint = t(
        "Bookmark albums to see them here",
        "احفظ الألبومات لتظهر هنا"
    )
    val sharedViaMimusic = t("Shared via MiMusic", "مُشارك عبر MiMusic")
    val playbackHistory = t("Listening history", "سجل الاستماع")
    val mostPlayed = t("Most played", "الأكثر تشغيلًا")
    val onDevice = t("On this device", "على الجهاز")
    val audioQuality = t("Audio quality", "جودة الصوت")
    val audioQualityAuto = t("Auto", "تلقائي")
    val audioQualityHigh = t("High", "عالية")
    val audioQualityMedium = t("Medium", "متوسطة")
    val audioQualityLow = t("Low / Data saver", "منخفضة / توفير البيانات")
    val crossfade = t("Crossfade", "تلاشي بين الأغاني")
    val crossfadeDescription = t("Fade out the current song into the next one", "تلاشٍ ناعم من الأغنية الحالية إلى التالية")
    val inAppEqualizer = t("In-app equalizer", "معادل داخل التطبيق")
    val inAppEqualizerDescription = t("Use a built-in preset instead of the system equalizer", "استخدم إعدادًا جاهزًا بدل معادل النظام")
    val bassBoost = t("Bass boost", "تعزيز الباس")
    val wifiOnlyDownload = t("Wi-Fi downloads only", "التنزيل عبر Wi-Fi فقط")
    val wifiOnlyDownloadDescription = t("Don't start downloads on mobile data", "لا تبدأ التنزيل على بيانات الجوال")
    val offlineMode = t("Offline mode", "وضع بدون اتصال")
    val offlineModeDescription = t("Play downloaded and on-device songs only", "تشغيل التنزيلات وملفات الجهاز فقط")
    val carMode = t("Car mode", "وضع السيارة")
    val carModeDescription = t("Larger player buttons while driving", "أزرار أكبر في المشغّل أثناء القيادة")
    val lyricsSize = t("Lyrics size", "حجم الكلمات")
    val appLock = t("Lock with device PIN", "قفل برقم الجهاز")
    val appLockDescription = t("Ask for the phone lock when opening MiMusic", "اطلب قفل الهاتف عند فتح مي ميوزك")
    val hideRecents = t("Hide from recents screenshots", "إخفاء لقطات التطبيقات")
    val hideRecentsDescription = t("Block screenshots of MiMusic in the recents screen", "منع لقطات شاشة مي ميوزك في قائمة التطبيقات")
    val smartShuffle = t("Smart shuffle", "خلط ذكي")
    val smartShuffleDescription = t("Avoid playing the same artist twice in a row", "تجنب تشغيل نفس الفنان مرتين متتاليتين")
    val abLoop = t("A–B loop", "تكرار مقطع")
    val markA = t("Mark A", "تحديد أ")
    val markB = t("Mark B", "تحديد ب")
    val clearLoop = t("Clear loop", "إلغاء التكرار")
    val playerLock = t("Lock player", "قفل المشغّل")
    val unlockPlayer = t("Tap to unlock", "اضغط للفتح")
    val similarSongs = t("Similar songs", "أغانٍ مشابهة")
    val editMetadata = t("Edit title", "تعديل العنوان")
    val exportM3u = t("Export M3U", "تصدير M3U")
    val importM3u = t("Import M3U / CSV", "استيراد M3U / CSV")
    val exportCsv = t("Export CSV", "تصدير CSV")
    val importPlaylistFile = t("Import playlist file", "استيراد قائمة من ملف")
    val importPlaylistHint = t(
        "YouTube Music, Spotify, or Anghami M3U/CSV",
        "يوتيوب ميوزك أو سبوتيفاي أو أنغامي بصيغة M3U أو CSV"
    )
    val importFinished: (Int) -> String = { count ->
        t("Imported $count songs", "تم استيراد $count أغنية")
    }
    val importEmpty = t("No songs found in this file", "لا توجد أغاني في هذا الملف")
    val importingPlaylist = t("Importing playlist…", "جاري استيراد القائمة…")
    val exportFile = t("Save file", "حفظ الملف")
    val continueRecitation = t("Continue recitation", "متابعة التلاوة")
    val shareCard = t("Share card", "مشاركة كارت")
    val goldTheme = t("Gold", "ذهبي")
    val settingsSearch = t("Search settings", "بحث في الإعدادات")
    val onboardingTitle = t("Welcome to MiMusic", "أهلًا بك في مي ميوزك")
    val onboardingBody = t(
        "Arabic is the default language. Pick a nav style later in Appearance.",
        "العربية هي لغة الواجهة. يمكنك تغيير شكل القائمة لاحقًا من المظهر."
    )
    val gotIt = t("Got it", "حسنًا")
    val eqPreset = t("Equalizer preset", "إعداد المعادل")
    val search = t("Search", "بحث")
    val thisWeek = t("Played this week", "استُمع إليه هذا الأسبوع")
    val shortFavorites = t("Short favorites", "مفضّل قصير")
    val quranMishary = t("Mishary", "مشاري")
    val quranMinshawi = t("Minshawi", "المنشاوي")
    val quranHosary = t("Al-Husary", "الحصري")
    val quranSudais = t("As-Sudais", "السديس")
    val continueListening = t("Continue listening", "كمّل اللي وقفت عنده")
    val refresh = t("Refresh", "تحديث")
    val pitch = t("Pitch", "الطبقة")
    val lowPowerMode = t("Low power mode", "توفير الطاقة")
    val lowPowerModeDescription = t(
        "Lower quality, no crossfade, and smaller artwork to save battery",
        "جودة أقل، بدون تلاشي، وصور أصغر لتوفير البطارية"
    )
    val chargingOnlyDownload = t("Charge to download", "التنزيل أثناء الشحن")
    val chargingOnlyDownloadDescription = t(
        "Start downloads only while the phone is charging",
        "ابدأ التنزيل فقط أثناء شحن الهاتف"
    )
    val pictureInPicture = t("Picture-in-picture", "صورة داخل صورة")
    val pictureInPictureDescription = t(
        "Keep the player in a small window when leaving the app",
        "أبقِ المشغّل في نافذة صغيرة عند مغادرة التطبيق"
    )
    val dataUsage = t("Data used", "استهلاك البيانات")
    val crashLog = t("Send problem log", "إرسال سجل المشكلة")
    val crashLogDescription = t(
        "The crash report stays on this device until you send it",
        "تقرير العطل يفضل على الجهاز لحد ما تبعتّه"
    )
    val crashLogEmpty = t("No crash log yet", "لا يوجد سجل أعطال بعد")
    val removeDownloadConfirm = t(
        "Remove this downloaded song from the device?",
        "هل تريد حذف هذه الأغنية المنزّلة من الجهاز؟"
    )
    val clearDownloadsConfirm = t(
        "Remove every downloaded song?",
        "هل تريد حذف كل الأغاني المنزّلة؟"
    )
    val playlistCover = t("Playlist cover", "غلاف القائمة")
    val playlistFolderHint = t(
        "Tip: name it Folder / Playlist to group lists",
        "تلميح: اكتب مجلد / اسم القائمة لتجميع القوائم"
    )
    val themeSpotify = t("Spotify green", "أخضر سبوتيفاي")
    val themeYouTube = t("YouTube dark", "يوتيوب داكن")
    val enterPictureInPicture = t("Pop-out player", "مشغّل عائم")
    val extraSources = t("Extra sources", "مصادر زيادة")
    val extraSourcesHint = t(
        "Search SoundCloud, subscribed podcasts, and Jellyfin. Add feeds and a server in Other settings.",
        "ابحث في ساوندكلاود والبودكاست وجيليفين. أضف الخلاصات والسيرفر من الإعدادات الأخرى."
    )
    val extraSourcesDescription = t(
        "SoundCloud, podcast RSS feeds, and a Jellyfin server",
        "ساوندكلاود وخلاصات بودكاست وسيرفر جيليفين"
    )
    val listenBrainz = t("ListenBrainz", "ListenBrainz")
    val listenBrainzDescription = t(
        "Scrobble songs you play to your ListenBrainz profile",
        "أرسل ما تستمع إليه إلى حساب ListenBrainz"
    )
    val listenBrainzToken = t("ListenBrainz token", "رمز ListenBrainz")
    val listenBrainzTokenHint = t(
        "Paste the user token from listenbrainz.org/settings",
        "الصق الرمز من listenbrainz.org/settings"
    )
    val podcastFeeds = t("Podcast feeds", "خلاصات البودكاست")
    val podcastFeedsHint = t(
        "One RSS URL per line",
        "رابط RSS في كل سطر"
    )
    val jellyfin = t("Jellyfin", "جيليفين")
    val jellyfinServer = t("Jellyfin server", "سيرفر جيليفين")
    val jellyfinUser = t("Jellyfin username", "اسم مستخدم جيليفين")
    val jellyfinPassword = t("Jellyfin password", "كلمة مرور جيليفين")
    val jellyfinConnect = t("Connect Jellyfin", "الاتصال بجيليفين")
    val jellyfinConnected = t("Jellyfin connected", "تم الاتصال بجيليفين")
    val jellyfinConnectFailed = t("Couldn't connect to Jellyfin", "تعذر الاتصال بجيليفين")
    val videoLyrics = t("Lyrics on video", "كلمات على الفيديو")
    val videoLyricsDescription = t(
        "While watching a music video, show the current lyric line on it",
        "أثناء مشاهدة الفيديو، اعرض السطر الحالي من الكلمات عليه"
    )
    val becauseYouListened = { name: String ->
        t("Because you listened to $name", "لأنك سمعت $name")
    }
    val fridayMoods = t("Friday moods", "مزاج الجمعة")
    val morningMoods = t("Morning moods", "مزاج الصباح")
    val eveningMoods = t("Evening moods", "مزاج الليل")
    val sources = t("Sources", "المصادر")
    val everything = t("All", "الكل")
    val podcasts = t("Podcasts", "بودكاست")
    val quran = t("Quran", "قرآن")
    val officialCharts = t("Official charts", "الرسوم الرسمية")
    val viewAlbum = t("View album", "عرض الألبوم")
    val viewPlaylist = t("View playlist", "عرض القائمة")
    val previousSong = t("Previous", "السابق")
    val nextSong = t("Next", "التالي")
    val likeSong = t("Like", "إعجاب")
    val moreOptions = t("More options", "المزيد")
    val selectItems = t("Select", "تحديد")
    val sortRecent = t("Recent", "الأحدث")
    val sortTitle = t("Title", "العنوان")
    val sortSize = t("Size", "الحجم")
    val downloadedSize = { size: String -> t("Downloaded $size", "التنزيلات $size") }
    val onboardingLanguage = t("Choose your language", "اختار اللغة")
    val onboardingNavigation = t("Choose how you move around", "اختار شكل التنقل")
    val onboardingRegion = t("Choose your country", "اختار بلدك")
    val onboardingNext = t("Next", "التالي")
    val onboardingBack = t("Back", "رجوع")
    val onboardingStart = t("Start listening", "ابدأ الاستماع")
    val statId = t("Id", "المعرّف")
    val statItag = t("Itag", "الوسم")
    val statBitrate = t("Bitrate", "معدل البت")
    val statSize = t("Size", "الحجم")
    val statCached = t("Cached", "مخزّن")
    val statLoudness = t("Loudness", "علو الصوت")
    val bassOff = t("Off", "إيقاف")
    val bassLow = t("Low", "خفيف")
    val bassMedium = t("Medium", "متوسط")
    val bassHigh = t("High", "قوي")
    val presetFlat = t("Flat", "مستوٍ")
    val presetBass = t("Bass", "باس")
    val presetVocal = t("Vocal", "صوت")
    val presetTreble = t("Treble", "حاد")
    val youtubeMusic = t("YouTube Music", "يوتيوب ميوزك")
    val youtubeCookie = t("YouTube Music cookie", "كوكي يوتيوب ميوزك")
    val youtubeCookieHint = t("Paste the cookie from music.youtube.com", "الصق الكوكي من music.youtube.com")
    val youtubeCookieWarning = t(
        "This cookie can access your YouTube account. It stays on this device and is sent only to YouTube. Likes are copied here and nothing is written back.",
        "الكوكي ده يقدر يدخل على حساب يوتيوب بتاعك. بيفضل على الجهاز ومش بيتبعت غير ليوتيوب. الإعجابات بتتنسخ هنا ومفيش حاجة بترجع ليوتيوب."
    )
    val importLikedSongs = t("Import liked songs", "استيراد الأغاني المعجب بها")
    val importLikedSongsDescription = t(
        "Copy your YouTube Music likes into this library. One way only.",
        "انسخ إعجابات يوتيوب ميوزك للمكتبة دي. اتجاه واحد بس."
    )
    fun importLikedSongsDone(count: Int) = t("Imported $count liked songs", "تم استيراد $count أغنية")
    val importLikedSongsEmpty = t("No liked songs came back. Check the cookie.", "مفيش أغاني رجعت. راجع الكوكي.")
    val scheduledBackup = t("Daily backup folder", "مجلد النسخ اليومي")
    val scheduledBackupPick = t(
        "Choose a folder. A copy is saved once a day, and the newest 5 are kept.",
        "اختار مجلد. النسخة بتتحفظ مرة في اليوم، وآخر 5 نسخ بس بتفضل."
    )
    val scheduledBackupOn = t(
        "A copy is saved once a day. The newest 5 are kept.",
        "النسخة بتتحفظ مرة في اليوم. آخر 5 نسخ بتفضل."
    )
    val importLatestBackup = t("Import newest backup", "استيراد أحدث نسخة")
    val importLatestBackupDescription = t(
        "Replace this library with the newest file in the backup folder, then close the app.",
        "استبدل المكتبة دي بأحدث ملف في مجلد النسخ، وبعدين التطبيق هيتقفل."
    )
    val noScheduledBackup = t("No backup file in that folder yet", "لسه مفيش ملف نسخ في المجلد ده")
    val stopScheduledBackup = t("Stop daily backup", "إيقاف النسخ اليومي")
    fun dailyMix(number: Int) = t("Daily mix $number", "مزيج اليوم $number")
    val cast = t("Cast", "البث")
    val castUnavailable = t(
        "Cast needs Google Play services and a Chromecast on this network",
        "البث محتاج خدمات جوجل وجهاز كرومكاست على الشبكة"
    )

    fun equalizerPresetName(value: it.vfsfitvnm.vimusic.utils.EqualizerPreset) = when (value) {
        it.vfsfitvnm.vimusic.utils.EqualizerPreset.Flat -> presetFlat
        it.vfsfitvnm.vimusic.utils.EqualizerPreset.Bass -> presetBass
        it.vfsfitvnm.vimusic.utils.EqualizerPreset.Vocal -> presetVocal
        it.vfsfitvnm.vimusic.utils.EqualizerPreset.Treble -> presetTreble
    }

    fun bassLevelName(value: it.vfsfitvnm.vimusic.utils.BassLevel) = when (value) {
        it.vfsfitvnm.vimusic.utils.BassLevel.Off -> bassOff
        it.vfsfitvnm.vimusic.utils.BassLevel.Low -> bassLow
        it.vfsfitvnm.vimusic.utils.BassLevel.Medium -> bassMedium
        it.vfsfitvnm.vimusic.utils.BassLevel.High -> bassHigh
    }

    fun audioQualityName(value: it.vfsfitvnm.vimusic.enums.AudioQuality) = when (value) {
        it.vfsfitvnm.vimusic.enums.AudioQuality.Auto -> audioQualityAuto
        it.vfsfitvnm.vimusic.enums.AudioQuality.High -> audioQualityHigh
        it.vfsfitvnm.vimusic.enums.AudioQuality.Medium -> audioQualityMedium
        it.vfsfitvnm.vimusic.enums.AudioQuality.Low -> audioQualityLow
    }
}
