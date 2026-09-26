package mihon.gradle.extensions

import io.kotest.matchers.shouldBe
import mihon.gradle.catalogProject
import org.gradle.api.Project
import org.gradle.api.internal.TaskInternal
import org.gradle.api.tasks.testing.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test as JUnitTest

internal class GateScopeTest {
    @JUnitTest
    fun allInScopeWithoutProperty() {
        catalogProject().isInGateScope() shouldBe true
    }

    @JUnitTest
    fun listedModulesAreInScope() {
        val root = ProjectBuilder.builder().build()
        val common = ProjectBuilder.builder().withName("common").withParent(root).build()
        common.extensions.extraProperties[GATE_MODULES_PROPERTY] = ":source-local, :common"
        common.isInGateScope() shouldBe true
    }

    @JUnitTest
    fun unlistedModulesAreOutOfScope() {
        val root = ProjectBuilder.builder().build()
        val common = ProjectBuilder.builder().withName("common").withParent(root).build()
        common.extensions.extraProperties[GATE_MODULES_PROPERTY] = ":source-local"
        common.isInGateScope() shouldBe false
    }

    @JUnitTest
    fun testTasksRunOnlyInScope() {
        val project = catalogProject()
        project.extensions.extraProperties[GATE_MODULES_PROPERTY] = ":elsewhere"
        project.configureTest()
        project.unitTest().let { it.onlyIf.isSatisfiedBy(it) } shouldBe false
        val scoped = catalogProject()
        scoped.configureTest()
        scoped.unitTest().let { it.onlyIf.isSatisfiedBy(it) } shouldBe true
    }

    @JUnitTest
    fun theStaticPhaseRunsNoTests() {
        val project = catalogProject()
        project.extensions.extraProperties[GATE_PHASE_PROPERTY] = STATIC_PHASE
        project.gateRunsTests() shouldBe false
        project.configureTest()
        project.unitTest().let { it.onlyIf.isSatisfiedBy(it) } shouldBe false
        catalogProject().gateRunsTests() shouldBe true
    }

    @JUnitTest
    fun testForksComeFromTheProperty() {
        val serial = catalogProject()
        serial.configureTest()
        (serial.unitTest() as Test).maxParallelForks shouldBe 1
        val forked = catalogProject()
        forked.extensions.extraProperties[TEST_FORKS_PROPERTY] = "3"
        forked.configureTest()
        (forked.unitTest() as Test).maxParallelForks shouldBe 3
    }
}

private fun Project.unitTest(): TaskInternal = tasks.register("unitTest", Test::class.java).get()
