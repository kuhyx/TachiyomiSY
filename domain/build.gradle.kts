plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
    alias(mihonx.plugins.lint)
    alias(mihonx.plugins.coverage)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "tachiyomi.domain"

    // Robolectric (StorageManager's UniFile/Context, Release's Build.SUPPORTED_ABIS).
    testOptions.unitTests.isIncludeAndroidResources = true
}

kover {
    reports {
        filters {
            excludes {
                // Reified inline stubs only throw; see InlinedOnly's KDoc.
                annotatedBy("tachiyomi.domain.util.InlinedOnly")
            }
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

dependencies {
    implementation(projects.sourceApi)
    implementation(projects.core.common)

    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.serialization)

    implementation(libs.unifile)

    api(libs.sqldelight.androidxPaging)

    compileOnly(platform(libs.androidx.compose.bom))
    compileOnly(libs.androidx.compose.runtimeAnnotation)

    // SY -->
    implementation(libs.injekt)
    // SY <--

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Robolectric has no JUnit 5 runner: its tests are JUnit 4 classes run
    // by the vintage engine next to the Jupiter ones.
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage)
}
