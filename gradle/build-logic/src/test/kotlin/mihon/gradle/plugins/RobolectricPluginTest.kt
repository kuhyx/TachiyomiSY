package mihon.gradle.plugins

import com.android.build.api.dsl.LibraryExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import mihon.gradle.catalogProject
import org.gradle.api.internal.project.ProjectInternal
import org.junit.jupiter.api.Test

internal class RobolectricPluginTest {
    @Test
    fun addsTheHostTestStack() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidLibrary::class.java)
        project.plugins.apply(PluginRobolectric::class.java)
        val android = project.extensions.getByType(LibraryExtension::class.java)
        android.testOptions.unitTests.isIncludeAndroidResources shouldBe true
        val implementation = project.configurations.getByName("testImplementation").dependencies.map { it.name }
        implementation shouldContainAll listOf(
            "robolectric",
            "junit",
            "ui-test-junit4",
            "ui-test-manifest",
            "mockwebserver3",
            "kotlin-reflect",
            "kotlinx-coroutines-test",
        )
        val runtimeOnly = project.configurations.getByName("testRuntimeOnly").dependencies.map { it.name }
        runtimeOnly shouldContainAll listOf("junit-vintage-engine")
        project.configurations.getByName("debugImplementation").dependencies.isEmpty() shouldBe true
    }

    @Test
    fun appDebugGetsTestManifest() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidApplication::class.java)
        project.plugins.apply(PluginRobolectric::class.java)
        val debug = project.configurations.getByName("debugImplementation").dependencies.map { it.name }
        debug shouldContainAll listOf("ui-test-manifest")
    }

    @Test
    fun conscryptAndroidIsExcluded() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidLibrary::class.java)
        project.plugins.apply(PluginRobolectric::class.java)
        project.extensions.getByType(LibraryExtension::class.java).namespace = "mihon.test"
        (project as ProjectInternal).evaluate()
        val excluded = project.configurations.getByName("debugUnitTestRuntimeClasspath").excludeRules
        excluded.any { it.group == "org.conscrypt" && it.module == "conscrypt-android" } shouldBe true
        project.configurations.getByName("debugRuntimeClasspath").excludeRules.isEmpty() shouldBe true
    }
}
