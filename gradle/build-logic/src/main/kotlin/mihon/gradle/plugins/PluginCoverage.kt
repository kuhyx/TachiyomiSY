package mihon.gradle.plugins

import mihon.gradle.configurations.configureCoverage
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Kover with the 100% line and branch bound; applied per module once it gets there. */
public class PluginCoverage : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins {
                alias(libs.plugins.kover)
            }
            configureCoverage()
        }
    }
}
