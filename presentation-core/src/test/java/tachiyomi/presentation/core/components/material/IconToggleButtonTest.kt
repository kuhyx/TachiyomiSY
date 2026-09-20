package tachiyomi.presentation.core.components.material

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IconToggleButtonTest {
    @get:Rule
    val compose = createComposeRule()

    private var isChecked: Boolean? = null

    @Test
    fun uncheckedTogglesOn() {
        compose.setContent {
            MaterialTheme {
                IconToggleButton(
                    checked = false,
                    onCheckedChange = { isChecked = it },
                    imageVector = Icons.Filled.Favorite,
                    title = "Favourite",
                )
            }
        }
        compose.onNodeWithText("Favourite").assertIsDisplayed().assertIsOff()
        isChecked.shouldBeNull()
        compose.onNodeWithText("Favourite").performClick()
        isChecked shouldBe true
    }

    @Test
    fun checkedWithModifierTogglesOff() {
        compose.setContent {
            MaterialTheme {
                IconToggleButton(
                    checked = true,
                    onCheckedChange = { isChecked = it },
                    imageVector = Icons.Filled.Favorite,
                    title = "Favourite",
                    modifier = Modifier.testTag("toggle"),
                )
            }
        }
        compose.onNodeWithTag("toggle").assertIsOn().assertHeightIsEqualTo(48.dp).performClick()
        isChecked shouldBe false
    }
}
