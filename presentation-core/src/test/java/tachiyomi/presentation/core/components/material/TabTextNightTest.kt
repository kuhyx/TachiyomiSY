package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The dark-theme arm of the pill alpha; the qualifier flips [isSystemInDarkTheme]. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "night")
internal class TabTextNightTest {
    @get:Rule
    val compose = createComposeRule()

    private var isDark = false

    @Test
    fun withBadgeInTheDark() {
        compose.setContent {
            MaterialTheme {
                isDark = isSystemInDarkTheme()
                TabText(text = "Updates", badgeCount = 12)
            }
        }
        isDark shouldBe true
        compose.onNodeWithText("Updates").assertIsDisplayed()
        compose.onNodeWithText("12").assertIsDisplayed()
    }
}
