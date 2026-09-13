package mihon.gradle.extensions

import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

/** Applies a plugin by its catalog alias. */
public fun PluginManager.alias(notation: Provider<PluginDependency>) {
    apply(notation.get().pluginId)
}
