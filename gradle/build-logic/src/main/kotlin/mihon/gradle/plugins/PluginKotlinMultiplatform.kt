package mihon.gradle.plugins

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import mihon.gradle.configurations.configureKotlin
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.configureTest
import mihon.gradle.extensions.coreLibraryDesugaring
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.mihonx
import mihon.gradle.extensions.plugins
import mihon.gradle.extensions.releaseOf
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Kotlin Multiplatform module with an Android target sharing the base SDK configuration. */
public class PluginKotlinMultiplatform : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val sdk = mihonx.versions.android.sdk
            plugins {
                alias(libs.plugins.android.kmp.library)
                alias(libs.plugins.kotlin.multiplatform)
            }

            configureKotlin()
            configureTest()

            kotlin {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                applyDefaultHierarchyTemplate()

                android {
                    minSdk = sdk.min.get().toInt()
                    compileSdk {
                        version = releaseOf(sdk.compile)
                    }
                    enableCoreLibraryDesugaring = true
                }
            }

            dependencies {
                coreLibraryDesugaring(libs.android.desugar)
            }
        }
    }
}

private fun Project.kotlin(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(block)
}

private fun KotlinMultiplatformExtension.android(block: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach(block)
}
