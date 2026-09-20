package tachiyomi.presentation.core.i18n

import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class LocalizeTest {
    @get:Rule
    val compose = createComposeRule()

    private var resolved = ""

    @Test
    fun plainString() {
        compose.setContent { resolved = stringResource(MR.strings.action_cancel) }
        compose.runOnIdle { resolved shouldBe "Cancel" }
    }

    @Test
    fun formattedString() {
        compose.setContent { resolved = stringResource(MR.strings.pref_relative_format_summary, "a", "b") }
        compose.runOnIdle { resolved shouldBe """"a" instead of "b"""" }
    }

    @Test
    fun plural() {
        compose.setContent { resolved = pluralStringResource(MR.plurals.relative_time, 1) }
        compose.runOnIdle { resolved shouldBe "Yesterday" }
    }

    @Test
    fun formattedPlural() {
        compose.setContent { resolved = pluralStringResource(MR.plurals.relative_time, 3, 3) }
        compose.runOnIdle { resolved shouldBe "3 days ago" }
    }
}
