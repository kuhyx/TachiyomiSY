package mihon.gradle.plugins

import mihon.gradle.extensions.alias
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.mihonx
import mihon.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Android test module (baseline profiles): the Android test plugin plus the shared base configuration. */
public class PluginAndroidTest : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins {
                alias(libs.plugins.android.test)
                alias(mihonx.plugins.android.base)
            }
        }
    }
}
