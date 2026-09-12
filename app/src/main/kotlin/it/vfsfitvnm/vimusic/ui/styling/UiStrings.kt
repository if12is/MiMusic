package it.vfsfitvnm.vimusic.ui.styling

import it.vfsfitvnm.vimusic.enums.AlbumSortBy
import it.vfsfitvnm.vimusic.enums.AppLanguage
import it.vfsfitvnm.vimusic.enums.ArtistSortBy
import it.vfsfitvnm.vimusic.enums.ColorPaletteMode
import it.vfsfitvnm.vimusic.enums.ColorPaletteName
import it.vfsfitvnm.vimusic.enums.PlaylistSortBy
import it.vfsfitvnm.vimusic.enums.SongSortBy
import it.vfsfitvnm.vimusic.enums.ThumbnailRoundness

class UiStrings(private val language: AppLanguage) {
    private val ar: Boolean
        get() = language == AppLanguage.Arabic

    private fun t(english: String, arabic: String) = if (ar) arabic else english

    val languageTitle = t("Language", "اللغة")
    val languageDescription = t("Arabic is the default interface language", "العربية هي لغة الواجهة الأساسية")

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
    }

    fun colorPaletteMode(value: ColorPaletteMode) = when (value) {
        ColorPaletteMode.Light -> themeLight
        ColorPaletteMode.Dark -> themeDark
        ColorPaletteMode.System -> themeSystem
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
}
