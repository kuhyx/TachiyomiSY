package mihon.gradle.plugins

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import mihon.gradle.catalogProject
import mihon.gradle.extensions.GATE_MODULES_PROPERTY
import org.gradle.api.Project
import org.gradle.api.internal.TaskInternal
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class LintCoveragePluginsTest {
    @Test
    fun lintConfiguresEverything() {
        val project = catalogProject()
        project.plugins.apply(PluginAndroidLibrary::class.java)
        project.plugins.apply(PluginLint::class.java)
        project.plugins.hasPlugin("io.gitlab.arturbosch.detekt") shouldBe true
        val detekt = project.extensions.getByType(DetektExtension::class.java)
        detekt.allRules shouldBe true
        detekt.buildUponDefaultConfig shouldBe false
        project.tasks.withType(Detekt::class.java).forEach {
            it.jvmTarget shouldBe "17"
            it.excludes shouldContain "**/exh/eh/tags/**"
        }
        val lint = project.extensions.getByType(LibraryExtension::class.java).lint
        lint.warningsAsErrors shouldBe true
        lint.abortOnError shouldBe true
        lint.checkAllWarnings shouldBe true
        lint.checkDependencies shouldBe false
        lint.checkReleaseBuilds shouldBe true
        val kotlin = project.extensions.getByType(KotlinBaseExtension::class.java)
        val options = (kotlin as HasConfigurableKotlinCompilerOptions<*>).compilerOptions
        options.allWarningsAsErrors.get() shouldBe true
        options.freeCompilerArgs.get().contains("-Xexplicit-api=strict") shouldBe true
    }

    @Test
    fun lintSkipsAbsentExtensions() {
        val project = catalogProject()
        project.plugins.apply(PluginLint::class.java)
        project.plugins.hasPlugin("io.gitlab.arturbosch.detekt") shouldBe true
        project.extensions.findByType(LibraryExtension::class.java) shouldBe null
    }

    @Test
    fun lintSkipsBareKotlinExtension() {
        val project: Project = catalogProject()
        project.extensions.add(KotlinBaseExtension::class.java, "kotlin", mockk<KotlinBaseExtension>())
        project.plugins.apply(PluginLint::class.java)
        project.plugins.hasPlugin("io.gitlab.arturbosch.detekt") shouldBe true
    }

    @Test
    fun lintConfiguresKmpAndroidTarget() {
        val project = catalogProject()
        project.plugins.apply(PluginKotlinMultiplatform::class.java)
        project.plugins.apply(PluginLint::class.java)
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val extensions = (kotlin as ExtensionAware).extensions
        val android = extensions.getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
        android.lint.warningsAsErrors shouldBe true
        android.lint.checkDependencies shouldBe false
    }

    @Test
    fun coveragePluginAppliesKover() {
        val project = catalogProject()
        project.plugins.apply(PluginCoverage::class.java)
        project.plugins.apply("base")
        project.plugins.hasPlugin("org.jetbrains.kotlinx.kover") shouldBe true
        (project.tasks.findByName("koverVerify") != null) shouldBe true
        project.tasks.getByName("check").dependsOn.contains("koverVerify") shouldBe true
    }

    @Test
    fun coverageVerifiesDebugForAnApp(@TempDir dir: File) {
        dir.resolve("src/main/aidl/mihon/app/shizuku").mkdirs()
        dir.resolve("src/main/aidl/mihon/app/shizuku/IShellInterface.aidl").writeText("interface IShellInterface {}")
        dir.resolve("src/main/aidl/mihon/app/shizuku/notes.txt").writeText("not an aidl file")
        val project = catalogProject(dir)
        project.plugins.apply(PluginAndroidApplication::class.java)
        project.plugins.apply(PluginCoverage::class.java)
        project.tasks.getByName("check").dependsOn.contains("koverVerifyDebug") shouldBe true
        project.tasks.getByName("check").dependsOn.contains("koverVerify") shouldBe false
        val kover = project.extensions.getByType(KoverProjectExtension::class.java)
        val excluded = kover.reports.filters.excludes
        excluded.classes.get() shouldContainAll listOf(
            "*.BuildConfig",
            "*.databinding.*",
            "mihon.app.shizuku.IShellInterface",
            "mihon.app.shizuku.IShellInterface$*",
        )
        excluded.annotatedBy.get() shouldContain "mihon.core.common.NativeBinding"
    }

    @Test
    fun coverageIsSkippedOutOfScope() {
        val project = catalogProject()
        project.extensions.extraProperties[GATE_MODULES_PROPERTY] = ":elsewhere"
        project.plugins.apply(PluginCoverage::class.java)
        project.plugins.apply("base")
        val verify: TaskInternal = project.tasks.withType(TaskInternal::class.java).getByName("koverVerify")
        verify.onlyIf.isSatisfiedBy(verify) shouldBe false
    }
}
