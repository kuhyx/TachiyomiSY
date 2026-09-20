package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class SettingsItemsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun headingFromResource() {
        compose.setContent { MaterialTheme { HeadingItem(MR.strings.action_cancel) } }
        compose.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun headingFromText() {
        compose.setContent { MaterialTheme { HeadingItem("Plain heading") } }
        compose.onNodeWithText("Plain heading").assertIsDisplayed()
    }

    @Test
    fun vectorIconItemClicks() {
        var clicks = 0
        compose.setContent {
            MaterialTheme { IconItem(label = "Vector", icon = Icons.Default.Star, onClick = { clicks++ }) }
        }
        compose.onNodeWithText("Vector").performClick()
        compose.runOnIdle { clicks shouldBe 1 }
    }

    @Test
    fun sortItemDescending() {
        var clicks = 0
        compose.setContent {
            MaterialTheme { SortItem(label = "Down", sortDescending = true, onClick = { clicks++ }) }
        }
        compose.onNodeWithText("Down").performClick()
        compose.runOnIdle { clicks shouldBe 1 }
    }

    @Test
    fun sortItemAscending() {
        compose.setContent {
            MaterialTheme { SortItem(label = "Up", sortDescending = false, onClick = {}) }
        }
        compose.onNodeWithText("Up").assertIsDisplayed()
    }

    @Test
    fun sortItemWithoutDirection() {
        compose.setContent {
            MaterialTheme { SortItem(label = "None", sortDescending = null, onClick = {}) }
        }
        compose.onNodeWithText("None").assertIsDisplayed()
    }

    @Test
    fun baseSortItemWithIcon() {
        var clicks = 0
        compose.setContent {
            MaterialTheme { BaseSortItem(label = "Base", icon = Icons.Default.Star, onClick = { clicks++ }) }
        }
        compose.onNodeWithText("Base").performClick()
        compose.runOnIdle { clicks shouldBe 1 }
    }

    @Test
    fun checkboxItemTogglesPref() {
        val pref = InMemoryPreferenceStore().getBoolean("k", false)
        compose.setContent { MaterialTheme { CheckboxItem(label = "Pref", pref = pref) } }
        compose.onNodeWithText("Pref").performClick()
        compose.runOnIdle { pref.get() shouldBe true }
    }

    @Test
    fun checkboxAndRadioItemsClick() {
        var checkboxClicks = 0
        var radioClicks = 0
        compose.setContent {
            MaterialTheme {
                Column {
                    CheckboxItem(label = "Check", checked = true, onClick = { checkboxClicks++ })
                    RadioItem(label = "Radio", selected = false, onClick = { radioClicks++ })
                }
            }
        }
        compose.onNodeWithText("Check").performClick()
        compose.onNodeWithText("Radio").performClick()
        compose.runOnIdle {
            checkboxClicks shouldBe 1
            radioClicks shouldBe 1
        }
    }

    @Test
    fun painterIconItemSelectedOrNot() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                Column {
                    IconItem(
                        label = "Selected",
                        icon = ColorPainter(Color.Red),
                        selected = true,
                        onClick = { clicks++ },
                    )
                    IconItem(label = "Plain", icon = ColorPainter(Color.Blue), selected = false, onClick = {})
                }
            }
        }
        compose.onNodeWithText("Selected").performClick()
        compose.onNodeWithText("Plain").assertIsDisplayed()
        compose.runOnIdle { clicks shouldBe 1 }
    }
}
