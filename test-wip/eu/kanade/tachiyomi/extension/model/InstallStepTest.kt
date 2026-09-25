package eu.kanade.tachiyomi.extension.model

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class InstallStepTest {
    @Test
    fun completedSteps() {
        InstallStep.Idle.isCompleted() shouldBe true
        InstallStep.Installed.isCompleted() shouldBe true
        InstallStep.Error.isCompleted() shouldBe true
    }

    @Test
    fun runningSteps() {
        InstallStep.Pending.isCompleted() shouldBe false
        InstallStep.Downloading.isCompleted() shouldBe false
        InstallStep.Installing.isCompleted() shouldBe false
    }

    @Test
    fun entryOrder() {
        InstallStep.entries.map { it.name } shouldContainExactly listOf(
            "Idle",
            "Pending",
            "Downloading",
            "Installing",
            "Installed",
            "Error",
        )
        InstallStep.valueOf("Pending").ordinal shouldBe 1
    }
}
