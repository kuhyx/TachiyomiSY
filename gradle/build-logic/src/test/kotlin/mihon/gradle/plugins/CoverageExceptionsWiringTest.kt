package mihon.gradle.plugins

import io.kotest.matchers.shouldBe
import mihon.gradle.catalogProject
import mihon.gradle.extensions.GATE_MODULES_PROPERTY
import mihon.gradle.tasks.VerifyCoverageExceptionsTask
import org.gradle.api.Project
import org.gradle.api.internal.TaskInternal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class CoverageExceptionsWiringTest {
    @TempDir
    lateinit var dir: File

    private fun projectWithExceptions(configure: (Project) -> Unit = {}): Project {
        dir.resolve("coverage-exceptions.txt").writeText("p.Foo 1 sealed when\np.Bar 2 enum when\n")
        val project = catalogProject(dir)
        configure(project)
        return project
    }

    private fun Project.exceptionsTask(): VerifyCoverageExceptionsTask =
        tasks.withType(VerifyCoverageExceptionsTask::class.java).getByName("verifyCoverageExceptions")

    @Test
    fun libraryChecksTheTotalReport() {
        val project = projectWithExceptions()
        project.plugins.apply(PluginCoverage::class.java)
        project.plugins.apply("base")
        val task = project.exceptionsTask()
        task.report.get().asFile.name shouldBe "report.xml"
        task.exceptions.get().asFile.name shouldBe "coverage-exceptions.txt"
        project.tasks.getByName("check").dependsOn.any { it.toString().contains("verifyCoverageExceptions") } shouldBe
            true
    }

    @Test
    fun appChecksTheDebugReport() {
        val project = projectWithExceptions()
        project.plugins.apply(PluginAndroidApplication::class.java)
        project.plugins.apply(PluginCoverage::class.java)
        val task = project.exceptionsTask()
        task.report.get().asFile.name shouldBe "reportDebug.xml"
        task.dependsOn.contains("koverXmlReportDebug") shouldBe true
    }

    @Test
    fun checkIsSkippedOutOfScope() {
        val project = projectWithExceptions { it.extensions.extraProperties[GATE_MODULES_PROPERTY] = ":elsewhere" }
        project.plugins.apply(PluginCoverage::class.java)
        val task = project.exceptionsTask() as TaskInternal
        task.onlyIf.isSatisfiedBy(task) shouldBe false
    }

    @Test
    fun noFileMeansNoTask() {
        val project = catalogProject(dir)
        project.plugins.apply(PluginCoverage::class.java)
        project.tasks.findByName("verifyCoverageExceptions") shouldBe null
    }
}
