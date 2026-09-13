package mihon.gradle

import io.kotest.matchers.string.shouldMatch
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

private val ISO_UTC = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""")

internal class CommandsTest {
    private val project = ProjectBuilder.builder().build()

    @Test
    fun commitCountIsANumber() {
        project.getLatestCommitCount() shouldMatch Regex("""\d+""")
    }

    @Test
    fun commitShaIsShortHex() {
        project.getLatestCommitSha() shouldMatch Regex("[0-9a-f]{7,}")
    }

    @Test
    fun buildTimeFromCommitIsUtcIso() {
        project.getBuildTime(useLatestCommitTime = true) shouldMatch ISO_UTC
    }

    @Test
    fun buildTimeFromClockIsUtcIso() {
        project.getBuildTime(useLatestCommitTime = false) shouldMatch ISO_UTC
    }
}
