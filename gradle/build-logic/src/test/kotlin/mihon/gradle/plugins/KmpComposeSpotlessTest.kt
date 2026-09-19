package mihon.gradle.plugins

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import mihon.gradle.catalogProject
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.junit.jupiter.api.Test

internal class KmpComposeSpotlessTest {
    @Test
    fun kmpConfiguresAndroidTarget() {
        val project = catalogProject()
        project.plugins.apply(PluginKotlinMultiplatform::class.java)
        project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") shouldBe true
        project.plugins.hasPlugin("com.android.kotlin.multiplatform.library") shouldBe true
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val extensions = (kotlin as ExtensionAware).extensions
        val android = extensions.getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
        android.minSdk shouldBe 26
        android.enableCoreLibraryDesugaring shouldBe true
    }

    @Test
    fun kmpHostTestsSeeMainInternals() {
        val project = catalogProject()
        project.plugins.apply(PluginKotlinMultiplatform::class.java)
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val extensions = (kotlin as ExtensionAware).extensions
        val android = extensions.getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
        android.namespace = "mihon.test"
        android.withHostTest { }
        (project as ProjectInternal).evaluate()
        val compile = project.tasks.withType(KotlinCompile::class.java).getByName("compileAndroidHostTest")
        val fullJar = project.layout.buildDirectory
            .file("intermediates/full_jar/androidMain/createFullJarAndroidMain/full.jar")
            .get()
            .asFile
        compile.friendPaths.files shouldContain fullJar
    }

    @Test
    fun composeEnablesBuildFeature() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidLibrary::class.java)
        project.plugins.apply(PluginComposeAndroid::class.java)
        project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.compose") shouldBe true
        project.extensions.getByType(LibraryExtension::class.java).buildFeatures.compose shouldBe true
    }

    @Test
    fun spotlessRegistersExtension() {
        val project = catalogProject()
        project.plugins.apply(PluginSpotless::class.java)
        project.plugins.hasPlugin("com.diffplug.spotless") shouldBe true
        project.extensions.findByType(SpotlessExtension::class.java) shouldNotBe null
        (project as ProjectInternal).evaluate()
        listOf("spotlessKotlin", "spotlessKotlinGradle", "spotlessXml").forEach { name ->
            project.tasks.findByName(name) shouldNotBe null
        }
    }
}
