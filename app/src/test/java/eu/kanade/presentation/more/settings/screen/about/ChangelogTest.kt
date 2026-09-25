package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.ui.text.font.FontWeight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

private const val BULLET_PREFIX = "\u2022" + "\t\t"

internal class ChangelogTest {
    private fun changelog(bulleted: Boolean, vararg lines: String) = Changelog(
        bulletedList = bulleted,
        changelogs = listOf(
            ChangelogVersion(versionName = "1.2", changeDate = "today", text = lines.map { ChangelogText(it) }),
        ),
    )

    @Test
    fun plainListKeepsText() {
        val display = changelog(bulleted = false, "Fixed [b]bold[/b] thing").toDisplayChangelog()
        display.single().version shouldBe "1.2"
        val line = display.single().changelog.single()
        line.text shouldBe "Fixed bold thing"
        line.spanStyles.single().item.fontWeight shouldBe FontWeight.Bold
    }

    @Test
    fun bulletedListPrefixes() {
        val line = changelog(bulleted = true, "a]b[i[]").toDisplayChangelog().single().changelog.single()
        line.text shouldBe BULLET_PREFIX + "a]bi["
    }

    @Test
    fun strayCloseTagThrows() {
        val error = shouldThrow<IllegalStateException> {
            changelog(bulleted = false, "x[/b]").toDisplayChangelog()
        }
        error.message.orEmpty() shouldContain "1.2:0:3"
    }
}
