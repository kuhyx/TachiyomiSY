plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)

    alias(mihonx.plugins.spotless)
    alias(mihonx.plugins.lint)
    alias(mihonx.plugins.coverage)
}

android {
    namespace = "tachiyomi.presentation.widget"

    // Glance composables and the widget receivers run under Robolectric (no emulator).
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.domain)
    implementation(projects.presentationCore)
    api(projects.i18n)

    implementation(libs.androidx.glance.appWidget)
    implementation(libs.material)

    implementation(libs.coil.core)

    // SY -->
    implementation(libs.material)
    // SY <--

    api(libs.injekt)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Glance's unit-test harness and Robolectric are JUnit 4: run by the vintage engine.
    testImplementation(libs.androidx.glance.appWidgetTesting)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage)
}
