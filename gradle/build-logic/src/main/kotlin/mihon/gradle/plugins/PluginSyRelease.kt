package mihon.gradle.plugins

import mihon.gradle.configurations.configureSyRelease
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The kuhy fork's drop-in release build for the application module: the `kuhy` signing config
 * and the `foss` build type, driven by `-PsyReplaceUpstream` / `-PsyBuildNumber`
 * ([mihon.gradle.configurations.configureSyRelease]).
 */
public class PluginSyRelease : Plugin<Project> {
    override fun apply(target: Project) {
        target.configureSyRelease()
    }
}
