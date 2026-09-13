package mihon.gradle.transforms

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.gradle.catalogProject
import mihon.gradle.configurations.MULTI_RELEASE_STRIPPED
import mihon.gradle.configurations.stripMultiReleaseJars
import mihon.gradle.plugins.PluginAndroidLibrary
import mihon.gradle.plugins.PluginKotlinMultiplatform
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class StripMultiReleaseTransformTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun resolvedJarsComeOutStripped() {
        val project = catalogProject(dir)
        project.stripMultiReleaseJars()
        val jar = File(dir, "mr.jar").writeJar("a/B.class", "META-INF/versions/11/a/B.class")
        val probe = project.configurations.create("probe")
        project.dependencies.add(probe.name, project.files(jar))
        probe.attributes.getAttribute(MULTI_RELEASE_STRIPPED) shouldBe true
        val resolved = probe.incoming.artifactView { }.files.single()
        resolved.entryNames() shouldContainExactly listOf("a/B.class")
    }

    @Test
    fun bothPluginsMarkJarsUnstripped() {
        listOf(PluginAndroidLibrary::class.java, PluginKotlinMultiplatform::class.java).forEach { plugin ->
            val project = catalogProject()
            project.plugins.apply(plugin)
            val jarType = project.dependencies.artifactTypes.getByName(ArtifactTypeDefinition.JAR_TYPE)
            jarType.attributes.getAttribute(MULTI_RELEASE_STRIPPED) shouldBe false
        }
    }
}
