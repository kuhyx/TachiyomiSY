package mihon.gradle.plugins

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import mihon.gradle.catalogProject
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.junit.jupiter.api.Test as JupiterTest

internal class AndroidPluginsTest {
    @JupiterTest
    fun libraryPluginAppliesAgpAndBase() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidLibrary::class.java)
        project.plugins.hasPlugin("com.android.library") shouldBe true
        val android = project.extensions.getByType(LibraryExtension::class.java)
        android.defaultConfig.minSdk shouldBe 26
        android.compileSdk shouldBe 37
        android.compileOptions.isCoreLibraryDesugaringEnabled shouldBe true
    }

    @JupiterTest
    fun applicationPluginSetsTargetSdk() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidApplication::class.java)
        project.plugins.hasPlugin("com.android.application") shouldBe true
        val android = project.extensions.getByType(ApplicationExtension::class.java)
        android.defaultConfig.targetSdk shouldBe 36
    }

    @JupiterTest
    fun testPluginAppliesAgpTest() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidTest::class.java)
        project.plugins.hasPlugin("com.android.test") shouldBe true
    }

    @JupiterTest
    fun basePluginUsesJunitPlatform() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidLibrary::class.java)
        val test = project.tasks.register("unitTest", Test::class.java).get()
        (test.options is JUnitPlatformOptions) shouldBe true
        test.includes shouldBe setOf("**/*Test.class")
    }

    @JupiterTest
    fun testsRunOnTheTestJdk() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidLibrary::class.java)
        val test = project.tasks.register("unitTest", Test::class.java).get()
        val launcher = test.javaLauncher.get()
        launcher.metadata.languageVersion.asInt() shouldBe 21
    }

    @JupiterTest
    fun testTmpdirIsTheTaskTempDir() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidLibrary::class.java)
        val test = project.tasks.register("unitTest", Test::class.java).get()
        // Gradle keeps java.io.tmpdir with the JVM-managed properties, not in systemProperties.
        test.allJvmArgs shouldContain "-Djava.io.tmpdir=${test.temporaryDir.absolutePath}"
        test.temporaryDir.startsWith(project.layout.buildDirectory.get().asFile) shouldBe true
    }
}
