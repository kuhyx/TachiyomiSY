package mihon.gradle.plugins

import mihon.gradle.configurations.configureAppPackaging
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The application module's packaging, source sets, build features and compiler opt-ins
 * ([mihon.gradle.configurations.configureAppPackaging]).
 */
public class PluginAppPackaging : Plugin<Project> {
    override fun apply(target: Project) {
        target.configureAppPackaging()
    }
}
