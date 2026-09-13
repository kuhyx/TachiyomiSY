package mihon.gradle.plugins

import com.android.build.api.dsl.ApplicationExtension
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.mihonx
import mihon.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Android application module: the Android Gradle plugin plus the shared base configuration. */
public class PluginAndroidApplication : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins {
                alias(libs.plugins.android.application)
                alias(mihonx.plugins.android.base)
            }
            // Only applications have a target SDK; the base plugin stays free of an instanceof branch.
            val targetSdk = mihonx.versions.android.sdk.target
            extensions.configure<ApplicationExtension> {
                defaultConfig.targetSdk = targetSdk.get().toInt()
            }
        }
    }
}
