package mihon.gradle.plugins

import mihon.gradle.configurations.configureAndroidLint
import mihon.gradle.configurations.configureDetekt
import mihon.gradle.configurations.configureStrictKotlin
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The full lint stack: detekt with every rule, Android Lint with warnings as
 * errors, Kotlin warnings as errors and explicit API mode. A module applies
 * it in the same commit that makes it clean (AGENTS.md rollout order).
 */
public class PluginLint : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins {
                alias(libs.plugins.detekt)
            }
            configureDetekt(rootProject.file("config/detekt"))
            configureStrictKotlin()
            configureAndroidLint()
        }
    }
}
