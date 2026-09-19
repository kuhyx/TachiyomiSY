package mihon.gradle.plugins

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import mihon.gradle.configurations.configureKotlin
import mihon.gradle.configurations.stripMultiReleaseJars
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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private const val HOST_TEST_COMPILE_TASK = "compileAndroidHostTest"

private const val AGP_ANDROID_MAIN_FULL_JAR = "intermediates/full_jar/androidMain/createFullJarAndroidMain/full.jar"

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
            stripMultiReleaseJars()

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
            befriendMainFullJarInHostTests()

            dependencies {
                coreLibraryDesugaring(libs.android.desugar)
            }
        }
    }
}

// Host tests must see the module's internals, as an Android library's unit tests do. The KMP
// Android target lists the main classes directory as a friend but compiles the tests against
// AGP 9.4's full jar of those classes, which is not on the friend list, so every `internal`
// member reads as inaccessible; naming that jar as a friend too closes the gap.
private fun Project.befriendMainFullJarInHostTests() {
    val fullJar = layout.buildDirectory.file(AGP_ANDROID_MAIN_FULL_JAR)
    tasks.withType(KotlinCompile::class.java).matching { it.name == HOST_TEST_COMPILE_TASK }.configureEach {
        friendPaths.from(fullJar)
    }
}

private fun Project.kotlin(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(block)
}

private fun KotlinMultiplatformExtension.android(block: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach(block)
}
