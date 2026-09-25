import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("io.gitlab.arturbosch.detekt")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(envName: String, propertyName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

android {
    namespace = "it.vfsfitvnm.vimusic"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.mimusic.player"
        minSdk = 21
        targetSdk = 35
        versionCode = 41
        versionName = "0.7.9"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("MIMUSIC_KEYSTORE_FILE", "storeFile")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = signingValue("MIMUSIC_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("MIMUSIC_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("MIMUSIC_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    splits {
        abi {
            reset()
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "MiMusic Debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appName"] = "MiMusic"
            val releaseKeystore = signingConfigs.getByName("release").storeFile
            if (releaseKeystore != null && releaseKeystore.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
        jvmTarget = "17"
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        baseline = file("lint-baseline.xml")
        disable += setOf(
            "MissingTranslation",
            "ExtraTranslation",
            "IconDensities",
            "IconMissingDensityFolder",
            "ContentDescription",
            "HardcodedText",
            "ObsoleteLintCustomCheck",
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "OldTargetApi",
            "UnusedResources",
            "RtlSymmetry",
            "RtlHardcoded",
            "PluralsCandidate",
            "Overdraw",
            "SyntheticAccessor",
            "VectorPath",
            "NotifyDataSetChanged"
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("detekt-baseline.xml")
}

dependencies {
    implementation(projects.composePersist)
    implementation(projects.composeRouting)
    implementation(projects.composeReordering)

    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ripple)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.coil)

    implementation(libs.palette)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.security.crypto)

    implementation(libs.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.cast)
    implementation(libs.work.runtime)
    implementation(libs.acra.core)

    implementation(libs.room)
    ksp(libs.room.compiler)

    implementation(projects.innertube)
    implementation(projects.kugou)

    coreLibraryDesugaring(libs.desugaring)

    testImplementation(testLibs.junit)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}

composeCompiler {
    enableStrongSkippingMode = true
}
