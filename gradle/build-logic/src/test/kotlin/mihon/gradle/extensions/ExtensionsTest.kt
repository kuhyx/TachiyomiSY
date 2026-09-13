package mihon.gradle.extensions

import com.android.build.api.dsl.CompileSdkReleaseSpec
import com.android.build.api.dsl.CompileSdkSpec
import com.android.build.api.dsl.CompileSdkVersion
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import mihon.gradle.catalogProject
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.plugin.use.PluginDependency
import org.junit.jupiter.api.Test

internal class ExtensionsTest {
    private val project = catalogProject()

    private fun releaseSpec(): Pair<CompileSdkSpec, CompileSdkReleaseSpec> {
        val release = mockk<CompileSdkReleaseSpec>(relaxed = true)
        val spec = mockk<CompileSdkSpec>()
        val block = slot<CompileSdkReleaseSpec.() -> Unit>()
        every { spec.release(any<Int>(), capture(block)) } answers {
            block.captured(release)
            mockk<CompileSdkVersion>()
        }
        return spec to release
    }

    @Test
    fun releaseOfParsesMajorAndMinor() {
        val (spec, release) = releaseSpec()
        spec.releaseOf(project.provider { "37.1" })
        verify { spec.release(37, any()) }
        verify { release.minorApiLevel = 1 }
    }

    @Test
    fun releaseOfWithoutMinor() {
        val (spec, release) = releaseSpec()
        spec.releaseOf(project.provider { "36" })
        verify { spec.release(36, any()) }
        verify { release.minorApiLevel = null }
    }

    @Test
    fun dependencyHelpersAddToConfigs() {
        listOf("api", "coreLibraryDesugaring", "debugApi", "implementation")
            .forEach { project.configurations.create(it) }
        val scope = DependencyHandlerScope.of(project.dependencies)
        val libs = project.extensions.getByType(LibrariesForLibs::class.java)
        scope.api(libs.android.desugar)
        scope.coreLibraryDesugaring(libs.android.desugar)
        scope.debugApi(libs.android.desugar)
        scope.implementation(libs.android.desugar)
        scope.implementation(project)
        val bundle = object :
            ExternalModuleDependencyBundle,
            MutableList<MinimalExternalModuleDependency> by mutableListOf(libs.android.desugar.get()) {}
        val bundleProvider = project.objects.property(ExternalModuleDependencyBundle::class.java).value(bundle)
        scope.implementation(bundleProvider)
        val implementation = project.configurations.getByName("implementation").dependencies
        implementation.withType(ProjectDependency::class.java).size shouldBe 1
        implementation.size shouldBe 3
    }

    @Test
    fun aliasAppliesThePluginId() {
        val dependency = mockk<PluginDependency>()
        every { dependency.pluginId } returns "base"
        project.pluginManager.alias(project.provider { dependency })
        project.plugins.hasPlugin("base") shouldBe true
    }
}
