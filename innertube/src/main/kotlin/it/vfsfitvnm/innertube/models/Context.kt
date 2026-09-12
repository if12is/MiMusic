package it.vfsfitvnm.innertube.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Context(
    val client: Client,
    val thirdParty: ThirdParty? = null,
) {
    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val platform: String? = null,
        val hl: String = Context.hl,
        val gl: String = Context.gl,
        val visitorData: String? = null,
        val androidSdkVersion: Int? = null,
        val userAgent: String? = null,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val osName: String? = null,
        val osVersion: String? = null,
        @Transient
        val host: String = "www.youtube.com"
    )

    @Serializable
    data class ThirdParty(
        val embedUrl: String,
    )

    fun localized(
        language: String = hl,
        region: String = gl
    ) = copy(
        client = client.copy(hl = language, gl = region)
    )

    companion object {
        var hl: String = "ar"
        var gl: String = "EG"

        private const val USER_AGENT_WEB =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        val DefaultWeb: Context
            get() = Context(
                client = Client(
                    clientName = "WEB_REMIX",
                    clientVersion = "1.20260707.12.00",
                    platform = "DESKTOP",
                    hl = hl,
                    gl = gl,
                    userAgent = USER_AGENT_WEB,
                    host = "music.youtube.com"
                )
            )

        val DefaultWebWatch: Context
            get() = Context(
                client = Client(
                    clientName = "WEB",
                    clientVersion = "2.20260707.00.00",
                    platform = "DESKTOP",
                    hl = hl,
                    gl = gl,
                    userAgent = USER_AGENT_WEB,
                    host = "www.youtube.com"
                )
            )

        val DefaultAndroid: Context
            get() = Context(
                client = Client(
                    clientName = "ANDROID",
                    clientVersion = "21.26.364",
                    platform = "MOBILE",
                    hl = hl,
                    gl = gl,
                    androidSdkVersion = 30,
                    osName = "Android",
                    osVersion = "11",
                    userAgent = "com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip"
                )
            )

        val DefaultAndroidMusic: Context
            get() = Context(
                client = Client(
                    clientName = "ANDROID_MUSIC",
                    clientVersion = "7.27.52",
                    platform = "MOBILE",
                    hl = hl,
                    gl = gl,
                    androidSdkVersion = 30,
                    userAgent = "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 11) gzip",
                    host = "music.youtube.com"
                )
            )

        val DefaultAndroidVR: Context
            get() = Context(
                client = Client(
                    clientName = "ANDROID_VR",
                    clientVersion = "1.65.10",
                    hl = hl,
                    gl = gl,
                    deviceMake = "Oculus",
                    deviceModel = "Quest 3",
                    androidSdkVersion = 32,
                    osName = "Android",
                    osVersion = "12L",
                    userAgent = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
                )
            )

        val DefaultIos: Context
            get() = Context(
                client = Client(
                    clientName = "IOS",
                    clientVersion = "21.26.4",
                    hl = hl,
                    gl = gl,
                    deviceMake = "Apple",
                    deviceModel = "iPhone16,2",
                    osName = "iPhone",
                    osVersion = "18.3.2.22D82",
                    userAgent = "com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
                )
            )

        val DefaultAgeRestrictionBypass: Context
            get() = Context(
                client = Client(
                    clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
                    clientVersion = "2.0",
                    platform = "TV",
                    hl = hl,
                    gl = gl,
                    userAgent = "Mozilla/5.0 (PlayStation 4 5.55) AppleWebKit/601.2 (KHTML, like Gecko)"
                )
            )
    }
}
