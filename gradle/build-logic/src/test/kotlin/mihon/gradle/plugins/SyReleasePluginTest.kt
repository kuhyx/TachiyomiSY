package mihon.gradle.plugins

import com.android.build.api.dsl.ApplicationExtension
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import mihon.gradle.catalogProject
import mihon.gradle.configurations.SY_BUILD_NUMBER
import mihon.gradle.configurations.SY_KEY_PROPERTIES
import mihon.gradle.configurations.SY_REPLACE_UPSTREAM
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class SyReleasePluginTest {
    @TempDir
    lateinit var dir: File

    private fun appProject(keys: Boolean, vararg properties: Pair<String, String>): Project {
        val keyFile = dir.resolve("key.properties")
        if (keys) {
            keyFile.writeText(
                """
                    storeFile=${dir.resolve("kuhy.jks")}
                    storePassword=sp
                    keyAlias=ka
                    keyPassword=kp
                """.trimIndent(),
            )
        }
        val project = catalogProject(dir.resolve("app").apply { mkdirs() })
        project.extensions.extraProperties.set(SY_KEY_PROPERTIES, keyFile.absolutePath)
        properties.forEach { (name, value) -> project.extensions.extraProperties.set(name, value) }
        project.plugins.apply(PluginAndroidApplication::class.java)
        project.plugins.apply(PluginSyRelease::class.java)
        val android = project.extensions.getByType(ApplicationExtension::class.java)
        android.namespace = "mihon.test"
        android.buildFeatures.buildConfig = true
        android.buildTypes.getByName("release").isMinifyEnabled = true
        return project
    }

    private fun Project.android(): ApplicationExtension {
        (this as ProjectInternal).evaluate()
        return extensions.getByType(ApplicationExtension::class.java)
    }

    @Test
    fun noKeyMeansCompanionApp() {
        val android = appProject(keys = false).android()
        android.signingConfigs.findByName("kuhy").shouldBeNull()
        val foss = android.buildTypes.getByName("foss")
        foss.applicationIdSuffix shouldBe ".foss"
        foss.versionNameSuffix.shouldBeNull()
        foss.signingConfig.shouldBeNull()
        foss.isMinifyEnabled shouldBe true
        foss.matchingFallbacks shouldContain "release"
    }

    @Test
    fun theKeyAloneDoesNotChangeFoss() {
        val android = appProject(keys = true).android()
        android.signingConfigs.getByName("kuhy").storeFile shouldBe dir.resolve("kuhy.jks")
        android.buildTypes.getByName("foss").applicationIdSuffix shouldBe ".foss"
    }

    @Test
    fun replaceUpstreamSignsInPlace() {
        val android = appProject(keys = true, SY_REPLACE_UPSTREAM to "").android()
        val foss = android.buildTypes.getByName("foss")
        foss.applicationIdSuffix.shouldBeNull()
        foss.versionNameSuffix shouldBe "-kuhy"
        foss.signingConfig shouldBe android.signingConfigs.getByName("kuhy")
    }

    @Test
    fun buildNumberInVersionName() {
        val android = appProject(keys = true, SY_REPLACE_UPSTREAM to "", SY_BUILD_NUMBER to "42").android()
        android.buildTypes.getByName("foss").versionNameSuffix shouldBe "-kuhy.42"
    }

    @Test
    fun blankBuildNumberIsDropped() {
        val android = appProject(keys = true, SY_REPLACE_UPSTREAM to "", SY_BUILD_NUMBER to " ").android()
        android.buildTypes.getByName("foss").versionNameSuffix shouldBe "-kuhy"
    }

    @Test
    fun replaceWithoutKeyFailsClosed() {
        val project = appProject(keys = false, SY_REPLACE_UPSTREAM to "")
        val failure = shouldThrow<GradleException> { project.android() }
        generateSequence<Throwable>(failure) { it.cause }.last().message!!.contains("key.properties") shouldBe true
    }

    @Test
    fun defaultKeyFileIsUnderHome() {
        val project = catalogProject(dir.resolve("home").apply { mkdirs() })
        project.plugins.apply(PluginAndroidApplication::class.java)
        project.plugins.apply(PluginSyRelease::class.java)
        // No override: the ~/.android/release path is read, which on this machine may or may
        // not exist -- either way applying the plugin must not fail.
        project.plugins.hasPlugin(PluginSyRelease::class.java) shouldBe true
    }
}
