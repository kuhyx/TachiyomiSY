package mihon.gradle.extensions

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

private const val IMPLEMENTATION: String = "implementation"

/** `api` for a catalog entry, usable inside compiled plugins. */
public fun DependencyHandlerScope.api(dependencyNotation: Provider<MinimalExternalModuleDependency>) {
    add("api", dependencyNotation)
}

/** `coreLibraryDesugaring` for a catalog entry. */
public fun DependencyHandlerScope.coreLibraryDesugaring(dependencyNotation: Provider<MinimalExternalModuleDependency>) {
    add("coreLibraryDesugaring", dependencyNotation)
}

/** `debugApi` for a catalog entry. */
public fun DependencyHandlerScope.debugApi(dependencyNotation: Provider<MinimalExternalModuleDependency>) {
    add("debugApi", dependencyNotation)
}

/** `implementation` for a catalog bundle. */
@JvmName("implementationBundle")
public fun DependencyHandlerScope.implementation(dependencyNotation: Provider<ExternalModuleDependencyBundle>) {
    add(IMPLEMENTATION, dependencyNotation)
}

/** `implementation` for a catalog entry. */
public fun DependencyHandlerScope.implementation(dependencyNotation: Provider<MinimalExternalModuleDependency>) {
    add(IMPLEMENTATION, dependencyNotation)
}

/** `implementation` for another module of this build. */
public fun DependencyHandlerScope.implementation(dependencyNotation: Project) {
    add(IMPLEMENTATION, dependencyNotation)
}
