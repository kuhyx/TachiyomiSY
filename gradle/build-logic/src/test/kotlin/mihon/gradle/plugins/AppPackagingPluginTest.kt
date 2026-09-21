package mihon.gradle.plugins

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.gradle.internal.dsl.AbiSplitOptions
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import mihon.gradle.catalogProject
import mihon.gradle.configurations.APP_ABIS
import mihon.gradle.configurations.APP_OPT_INS
import mihon.gradle.configurations.EXCLUDED_RESOURCES
import mihon.gradle.configurations.registerShortcutsTask
import mihon.gradle.tasks.ReplaceShortcutsPlaceholderTask
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class AppPackagingPluginTest {
    @TempDir
    lateinit var dir: File

    private fun appProject(): Project {
        val project = catalogProject(dir.resolve("app").apply { mkdirs() })
        project.plugins.apply(PluginAndroidApplication::class.java)
        project.plugins.apply(PluginAppPackaging::class.java)
        val android = project.extensions.getByType(ApplicationExtension::class.java)
        android.namespace = "mihon.test"
        android.flavorDimensions += "default"
        android.productFlavors.create("standard") { dimension = "default" }
        return project
    }

    @Test
    fun packagingLandsOnAndroid() {
        val android = appProject().extensions.getByType(ApplicationExtension::class.java)
        android.splits.abi.isEnable shouldBe true
        android.splits.abi.isUniversalApk shouldBe true
        (android.splits.abi as AbiSplitOptions).applicableFilters shouldContainAll APP_ABIS
        android.packaging.jniLibs.keepDebugSymbols shouldContain "**/libquickjs.so"
        android.packaging.resources.excludes shouldContainAll EXCLUDED_RESOURCES
        android.dependenciesInfo.includeInApk shouldBe false
        android.buildFeatures.viewBinding shouldBe true
        android.buildFeatures.buildConfig shouldBe true
        android.buildFeatures.aidl shouldBe true
        android.lint.checkDependencies shouldBe true
        android.sourceSets.getByName("release").java.directories shouldContain "src/release/java"
        android.sourceSets.getByName("debug").java.directories shouldContain "src/debug/java"
    }

    @Test
    fun optInsReachTheKotlinCompiler() {
        val kotlin = appProject().extensions.getByType(KotlinAndroidProjectExtension::class.java)
        kotlin.compilerOptions.freeCompilerArgs.get() shouldContainAll APP_OPT_INS.map { "-opt-in=$it" }
    }

    @Test
    fun everyVariantGetsAShortcutsTask() {
        val project = appProject()
        (project as ProjectInternal).evaluate()
        val task = project.tasks.withType(ReplaceShortcutsPlaceholderTask::class.java)
            .getByName("replaceStandardDebugShortcutPlaceholder")
        task.applicationId.get() shouldBe "mihon.test"
        task.shortcutsFile.get().asFile shouldBe project.projectDir.resolve("src/main/shortcuts.xml")
        project.tasks.withType(ReplaceShortcutsPlaceholderTask::class.java).names shouldContain
            "replaceStandardReleaseShortcutPlaceholder"
    }

    @Test
    fun noResourcesMeansNoTask() {
        val project = catalogProject(dir)
        val variant = mockk<ApplicationVariant> {
            every { sources.res } returns null
            every { name } returns "bare"
        }
        project.registerShortcutsTask(variant)
        project.tasks.withType(ReplaceShortcutsPlaceholderTask::class.java).isEmpty() shouldBe true
    }
}
