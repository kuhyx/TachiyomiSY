package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val LIST_TAG = "list"

/** A scrolling child inside the phone sheet hands its edge drags and flings to the sheet. */
@RunWith(RobolectricTestRunner::class)
internal class AdaptiveSheetNestedScrollTest {
    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0
    private var sheetModifier by mutableStateOf(Modifier.testTag("first"))

    private fun setSheetWithList() {
        compose.setContent {
            MaterialTheme {
                AdaptiveSheet(
                    isTabletUi = false,
                    enableSwipeDismiss = true,
                    onDismissRequest = { dismissed++ },
                    modifier = sheetModifier,
                ) {
                    LazyColumn(modifier = Modifier.height(200.dp).testTag(LIST_TAG)) {
                        items(count = 50) { index ->
                            Text(text = "Row $index", modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun flingInsideListKeepsSheetShown() {
        setSheetWithList()
        compose.onNodeWithTag(LIST_TAG, useUnmergedTree = true).performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithText("Row 0").assertDoesNotExist()
        compose.onNodeWithTag(LIST_TAG, useUnmergedTree = true).assertIsDisplayed()
        dismissed shouldBe 0
    }

    @Test
    fun dragPastTopFollowsThenSettles() {
        setSheetWithList()
        compose.onNodeWithTag(LIST_TAG, useUnmergedTree = true).performTouchInput { swipeDown() }
        compose.waitForIdle()
        compose.onNodeWithText("Row 0").assertIsDisplayed()
        compose.onNodeWithTag(LIST_TAG, useUnmergedTree = true).assertIsDisplayed()
        dismissed shouldBe 0
    }

    @Test
    fun keepsConnectionAcrossRecompose() {
        setSheetWithList()
        compose.runOnIdle { sheetModifier = Modifier.testTag("second") }
        compose.waitForIdle()
        compose.onNodeWithTag("second").assertIsDisplayed()
        compose.onNodeWithTag(LIST_TAG, useUnmergedTree = true).performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithText("Row 0").assertDoesNotExist()
        dismissed shouldBe 0
    }
}
