package mihon.gradle

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
    fun buildTimeSkipsNonBuildCommits() {
        // The newest commit touching only scripts/, .github/ or *.md must not move it.
        val excludes = listOf("*.md", "scripts", ".github", "docs", ".editorconfig")
            .joinToString(" ") { ":(top,exclude)$it" }
        val latestBuildInput = project.exec("git log -1 --format=%ct -- :/ $excludes")
        val expected = Instant.ofEpochSecond(latestBuildInput.toLong()).atOffset(ZoneOffset.UTC)
        project.getBuildTime(useLatestCommitTime = true) shouldBe
            expected.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
    }

    @Test
    fun buildTimeFromClockIsUtcIso() {
        project.getBuildTime(useLatestCommitTime = false) shouldMatch ISO_UTC
    }
}
