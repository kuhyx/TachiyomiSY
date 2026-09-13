package mihon.gradle.plugins

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.DefaultConfig
import mihon.gradle.configurations.configureKotlin
import mihon.gradle.configurations.stripMultiReleaseJars
import mihon.gradle.extensions.android
import mihon.gradle.extensions.configureTest
import mihon.gradle.extensions.coreLibraryDesugaring
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.mihonx
import mihon.gradle.extensions.releaseOf
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Shared Android configuration: SDK levels, NDK, desugaring, JUnit platform tests. */
public class PluginAndroidBase : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val sdk = mihonx.versions.android.sdk
            configureKotlin()
            configureTest()
            stripMultiReleaseJars()

            android {
                defaultConfig {
                    minSdk = sdk.min.get().toInt()
                    ndkVersion = mihonx.versions.android.ndk.get()
                }

                compileSdk {
                    version = releaseOf(sdk.compile)
                }

                compileOptions {
                    isCoreLibraryDesugaringEnabled = true
                }
            }

            dependencies {
                coreLibraryDesugaring(libs.android.desugar)
            }
        }
    }
}

private fun CommonExtension.defaultConfig(block: DefaultConfig.() -> Unit) {
    defaultConfig.apply(block)
}

private fun CommonExtension.compileOptions(block: CompileOptions.() -> Unit) {
    compileOptions.apply(block)
}
