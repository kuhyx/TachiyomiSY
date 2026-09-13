package mihon.gradle.plugins

import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.CommonExtension
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.android
import mihon.gradle.extensions.api
import mihon.gradle.extensions.debugApi
import mihon.gradle.extensions.implementation
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Jetpack Compose for an Android module: compiler plugin, BOM and the preview tooling. */
public class PluginComposeAndroid : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins {
                alias(libs.plugins.kotlin.compose.compiler)
            }

            android {
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                implementation(platform(libs.androidx.compose.bom))

                // Compose @Preview tooling
                api(libs.androidx.compose.uiToolingPreview)
                debugApi(libs.androidx.compose.uiTooling)
            }
        }
    }
}

private fun CommonExtension.buildFeatures(block: BuildFeatures.() -> Unit) {
    buildFeatures.apply(block)
}
