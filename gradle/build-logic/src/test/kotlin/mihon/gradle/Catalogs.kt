package mihon.gradle

import io.mockk.every
import io.mockk.mockk
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.accessors.dm.LibrariesForMihonx
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint
import org.gradle.api.internal.artifacts.dependencies.MinimalExternalModuleDependencyInternal
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

/** A ProjectBuilder project whose `libs` and `mihonx` catalogs are mocks with the real plugin ids. */
internal fun catalogProject(dir: File? = null): Project {
    val builder = ProjectBuilder.builder()
    if (dir != null) builder.withProjectDir(dir)
    val project = builder.build()
    project.extensions.add(LibrariesForLibs::class.java, "libs", mockLibs(project))
    project.extensions.add(LibrariesForMihonx::class.java, "mihonx", mockMihonx(project))
    return project
}

private fun Project.plugin(id: String): Provider<PluginDependency> = provider {
    mockk<PluginDependency> { every { pluginId } returns id }
}

private fun Project.library(group: String, name: String): Provider<MinimalExternalModuleDependency> = provider {
    val identifier = mockk<ModuleIdentifier>()
    every { identifier.group } returns group
    every { identifier.name } returns name
    mockk<MinimalExternalModuleDependencyInternal>(relaxed = true) {
        every { module } returns identifier
        every { version } returns "1.0.0"
        every { versionConstraint } returns DefaultMutableVersionConstraint("1.0.0")
    }
}

private fun mockLibs(project: Project): LibrariesForLibs = mockk {
    every { plugins.android.library } returns project.plugin("com.android.library")
    every { plugins.android.application } returns project.plugin("com.android.application")
    every { plugins.android.test } returns project.plugin("com.android.test")
    every { plugins.android.kmp.library } returns project.plugin("com.android.kotlin.multiplatform.library")
    every { plugins.kotlin.multiplatform } returns project.plugin("org.jetbrains.kotlin.multiplatform")
    every { plugins.kotlin.compose.compiler } returns project.plugin("org.jetbrains.kotlin.plugin.compose")
    every { plugins.spotless } returns project.plugin("com.diffplug.spotless")
    every { plugins.detekt } returns project.plugin("io.gitlab.arturbosch.detekt")
    every { plugins.kover } returns project.plugin("org.jetbrains.kotlinx.kover")
    every { android.desugar } returns project.library("com.android.tools", "desugar_jdk_libs")
    every { androidx.compose.bom } returns project.library("androidx.compose", "compose-bom")
    every { androidx.compose.uiToolingPreview } returns project.library("androidx.compose.ui", "ui-tooling-preview")
    every { androidx.compose.uiTooling } returns project.library("androidx.compose.ui", "ui-tooling")
    every { ktlint.bom } returns project.library("com.pinterest.ktlint", "ktlint-bom")
}

private fun mockMihonx(project: Project): LibrariesForMihonx = mockk {
    every { plugins.android.base } returns project.plugin("mihon.plugins.android.base")
    every { versions.android.sdk.min } returns project.provider { "26" }
    every { versions.android.sdk.target } returns project.provider { "36" }
    every { versions.android.sdk.compile } returns project.provider { "37.1" }
    every { versions.android.ndk } returns project.provider { "29.0.14206865" }
    every { versions.java } returns project.provider { "17" }
}
