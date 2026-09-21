package mihon.gradle.plugins

import com.android.build.api.dsl.LibraryExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import mihon.gradle.catalogProject
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
    }
}
