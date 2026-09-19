import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
    alias(mihonx.plugins.lint)
    alias(mihonx.plugins.coverage)
}

kotlin {
    android {
        namespace = "tachiyomi.source.local"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest {
            // Robolectric: a Context for moko strings, UniFile over real temp dirs.
            isIncludeAndroidResources = true
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(projects.sourceApi)
        api(projects.i18n)
        // SY -->
        api(projects.i18nSy)
        // SY <--

        implementation(libs.unifile)
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation(projects.core.common)
                implementation(projects.coreMetadata)

                // Move ChapterRecognition to separate module?
                implementation(projects.domain)

                implementation(libs.bundles.serialization)
            }
        }
        named("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
                // Robolectric has no JUnit 5 runner: its tests are JUnit 4 classes run
                // by the vintage engine next to the Jupiter ones.
                implementation(libs.robolectric)
                implementation(libs.junit4)
                runtimeOnly(libs.junit.vintage)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}
