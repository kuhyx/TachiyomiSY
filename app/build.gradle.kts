import mihon.gradle.getBuildTime
import mihon.gradle.getLatestCommitCount
import mihon.gradle.getLatestCommitSha

plugins {
    alias(mihonx.plugins.android.application)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
    alias(mihonx.plugins.lint)
    // SY fork (kuhy): the `kuhy` signing config and the `foss` drop-in build type
    // (`-PsyReplaceUpstream`, `-PsyBuildNumber`) live in gradle/build-logic.
    alias(mihonx.plugins.sy.release)
    // Splits, packaging, build features, opt-ins and the shortcuts task (gradle/build-logic).
    alias(mihonx.plugins.app.packaging)
    // Host tests under Robolectric with Compose's test rule and MockWebServer (gradle/build-logic).
    alias(mihonx.plugins.robolectric)

    kotlin("plugin.parcelize")

    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.androidx.baselineProfile)
    alias(libs.plugins.kotlin.serialization)

    id("io.github.ben-manes.versions")
}

if (gradle.startParameter.taskRequests.toString().contains("Release")) {
    pluginManager.apply {
        apply(libs.plugins.google.services.get().pluginId)
        apply(libs.plugins.firebase.crashlytics.get().pluginId)
    }
}

android {
    namespace = "eu.kanade.tachiyomi"

    defaultConfig {
        applicationId = "eu.kanade.tachiyomi.sy"

        versionCode = 81
        versionName = "1.13.2"

        buildConfigField("String", "UPSTREAM_VERSION", """"0.20.1"""")

        buildConfigField("String", "COMMIT_COUNT", "\"${getLatestCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getLatestCommitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime(useLatestCommitTime = false)}\"")
        buildConfigField("boolean", "INCLUDE_UPDATER", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        named("debug") {
            versionNameSuffix = "-${getLatestCommitCount()}"
            applicationIdSuffix = ".debug"
            isPseudoLocalesEnabled = true
        }
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isProfileable = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))

            buildConfigField("String", "BUILD_TIME", "\"${getBuildTime(useLatestCommitTime = true)}\"")
            buildConfigField("boolean", "INCLUDE_UPDATER", "true")
        }
        create("benchmark") {
            initWith(getByName("release"))

            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks.add("release")
            versionNameSuffix = "-benchmark"
            applicationIdSuffix = ".benchmark"

            buildConfigField("boolean", "INCLUDE_UPDATER", "false")
        }
    }

    sourceSets {
        getByName("benchmark").java.directories.add("src/debug/java")
        getByName("benchmark").res.directories.add("src/debug/res")
    }
}

baselineProfile {
    baselineProfileOutputDir = "baselineProfiles"
    mergeIntoMain = true
}

dependencies {
    baselineProfile(projects.baselineProfile)

    implementation(projects.i18n)
    // SY -->
    implementation(projects.i18nSy)
    // SY <--
    implementation(projects.core.common)
    implementation(projects.coreMetadata)
    implementation(projects.sourceApi)
    implementation(projects.sourceLocal)
    implementation(projects.data)
    implementation(projects.domain)
    implementation(projects.presentationCore)
    implementation(projects.presentationWidget)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.materialIcons)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animationGraphics)
    debugImplementation(libs.androidx.compose.uiTooling)
    implementation(libs.androidx.compose.uiToolingPreview)
    implementation(libs.androidx.compose.uiUtil)

    implementation(libs.androidx.interpolator)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.sqlite.bundled)
    // SY -->
    implementation(sylibs.sqlcipher)
    // SY <--

    implementation(libs.kotlin.reflect)

    implementation(libs.bundles.kotlinx.coroutines)

    implementation(libs.sqldelight.async)

    // AndroidX libraries
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.coreSplashScreen)
    implementation(libs.androidx.recyclerView)
    implementation(libs.androidx.viewPager)
    implementation(libs.androidx.profileInstaller)

    implementation(libs.bundles.androidx.lifecycle)

    // Job scheduling
    implementation(libs.androidx.work)

    // RxJava
    implementation(libs.rxJava)

    // Networking
    implementation(libs.bundles.okhttp)
    implementation(libs.okio)
    implementation(libs.conscrypt) // TLS 1.3 support for Android < 10

    // Data serialization (JSON, protobuf, xml)
    implementation(libs.bundles.serialization)

    // HTML parser
    implementation(libs.jsoup)

    // Disk
    implementation(libs.diskLruCache)
    implementation(libs.unifile)

    // Preferences
    implementation(libs.androidx.preference)

    // Dependency injection
    implementation(libs.injekt)

    // Image loading
    implementation(libs.bundles.coil)
    implementation(libs.subsamplingScaleImageView) {
        exclude(module = "image-decoder")
    }
    implementation(libs.image.decoder)

    // UI libraries
    implementation(libs.material)
    implementation(libs.flexibleAdapter)
    implementation(libs.photoView)
    implementation(libs.directionalViewPager) {
        exclude(group = "androidx.viewpager", module = "viewpager")
    }
    implementation(libs.composeRichEditor)
    implementation(libs.aboutLibraries.compose)
    implementation(libs.bundles.voyager)
    implementation(libs.composeMaterialMotion)
    implementation(libs.swipe)
    implementation(libs.composeWebview)
    implementation(libs.composeGrid)
    implementation(libs.reorderable)
    implementation(libs.bundles.markdown)
    implementation(libs.materialKolor)

    // Logging
    implementation(libs.logcat)

    // Crash reports/analytics
//    "standardImplementation"(platform(libs.firebase.bom))
//    "standardImplementation"(libs.firebase.analytics)
//    "standardImplementation"(libs.firebase.crashlytics)

    // Shizuku
    implementation(libs.bundles.shizuku)

    // String similarity
    implementation(libs.stringSimilarity)

    // Tests; the Robolectric stack comes from mihonx.plugins.robolectric
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)

    // For detecting memory leaks; see https://square.github.io/leakcanary/
    // debugImplementation(libs.leakCanary.android)
    implementation(libs.leakCanary.plumber)

    // SY -->
    // Firebase (EH)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Better logging (EH)
    implementation(sylibs.xlog)

    // RatingBar (SY)
    implementation(sylibs.ratingbar)
    implementation(sylibs.composeRatingbar)

    // Google drive
    implementation(sylibs.google.api.services.drive)
    implementation(sylibs.google.api.client.oauth)

    // Koin
    implementation(sylibs.koin.core)
    implementation(sylibs.koin.android)

    // ZXing Android Embedded
    implementation(sylibs.zxing.android.embedded)
}

// SY fork (kuhy): Kover at 100% (build-logic PluginCoverage) goes on unconditionally in the commit
// that brings this module to 100%; until then `-PsyAppCoverage` measures what is left, pushes stay green.
if (hasProperty("syAppCoverage")) {
    pluginManager.apply(mihonx.plugins.coverage.get().pluginId)
}
