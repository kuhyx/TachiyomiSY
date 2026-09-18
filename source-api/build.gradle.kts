import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
    alias(mihonx.plugins.lint)
    alias(mihonx.plugins.coverage)

    alias(libs.plugins.kotlin.serialization)

    id("io.github.ben-manes.versions")
}

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        namespace = "eu.kanade.tachiyomi.source"
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-proguard.pro")
            }
        }

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(libs.kotlinx.serialization.json)
        api(libs.injekt)
        api(libs.rxJava)
        api(libs.jsoup)

        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.runtime)

        // SY -->
        api(projects.i18n)
        api(projects.i18nSy)
        api(libs.kotlin.reflect)
        // SY <--
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation(projects.core.common)
                api(libs.androidx.preference)
            }
        }
        named("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
