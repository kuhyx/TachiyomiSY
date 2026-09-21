plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
    alias(mihonx.plugins.lint)
    alias(mihonx.plugins.coverage)

    alias(libs.plugins.kotlin.serialization)

    id("io.github.ben-manes.versions")
}

android {
    namespace = "eu.kanade.tachiyomi.core.common"

    // Robolectric (WebView, CookieManager, KeyStore, Bitmap*, ExifInterface,
    // UniFile all need a real android.jar behaviour under test).
    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}

dependencies {
    implementation(projects.i18n)
    // SY -->
    implementation(projects.i18nSy)
    // SY <--

    api(libs.logcat)

    api(libs.rxJava)

    api(libs.okhttp.core)
    api(libs.okhttp.logging)
    api(libs.okhttp.brotli)
    api(libs.okhttp.dnsOverHttps)
    api(libs.okio)

    implementation(libs.image.decoder)

    implementation(libs.unifile)
    implementation(libs.archive)

    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.serialization.jsonOkio)

    api(libs.androidx.preference)

    implementation(libs.jsoup)

    // Sort
    implementation(libs.natural.comparator)

    // JavaScript engine
    implementation(libs.quickJs)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Robolectric has no JUnit 5 runner: its tests are JUnit 4 classes run
    // by the vintage engine next to the Jupiter ones.
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.reflect)
    testRuntimeOnly(libs.junit.vintage)
    // Exercises network interceptors end to end; an application interceptor never reaches them.
    testImplementation(libs.okhttp.mockwebserver)

    // SY -->
    implementation(sylibs.xlog)
    implementation(libs.injekt)
    implementation(sylibs.exifinterface)
    // SY <--
}
