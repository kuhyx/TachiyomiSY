package mihon.gradle.plugins

import com.diffplug.gradle.spotless.SpotlessExtension
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** ktlint through Spotless for Kotlin, Gradle scripts and XML resources. */
public class PluginSpotless : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins {
                alias(libs.plugins.spotless)
            }

            // Configuration should be synced with [/gradle/build-logic/build.gradle.kts]
            val ktlintVersion = libs.ktlint.bom.get().version
            spotless {
                kotlin {
                    target("src/**/*.kt")
                    ktlint(ktlintVersion)
                    trimTrailingWhitespace()
                    endWithNewline()
                }

                kotlinGradle {
                    target("*.kts")
                    ktlint(ktlintVersion)
                    trimTrailingWhitespace()
                    endWithNewline()
                }

                format("xml") {
                    target("src/**/*.xml")
                    trimTrailingWhitespace()
                    endWithNewline()
                }
            }
        }
    }
}

private fun Project.spotless(block: SpotlessExtension.() -> Unit) {
    extensions.configure(block)
}
