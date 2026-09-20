package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val BODY = "Sheet body"
private const val SHEET_TAG = "sheet"
private const val OUTER_TAG = "outer"

/** The phone half of [AdaptiveSheet]: slides up, swipes down to dismiss. */
@RunWith(RobolectricTestRunner::class)
internal class AdaptiveSheetBottomTest {
    @get:Rule
    val compose = createComposeRule()

    private val owner = SheetBackOwner()
    private var dismissed = 0
    private var tick by mutableIntStateOf(0)

    @Composable
    private fun SheetBody() {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).testTag(SHEET_TAG)) {
            Text(text = BODY)
        }
    }

    @Composable
    private fun Sheet(swipe: Boolean, tagged: Boolean) {
        if (!tagged) {
            AdaptiveSheet(
                isTabletUi = false,
                enableSwipeDismiss = swipe,
                onDismissRequest = { dismissed++ },
            ) {
                SheetBody()
            }
        } else {
            AdaptiveSheet(
                isTabletUi = false,
                enableSwipeDismiss = swipe,
                onDismissRequest = { dismissed++ },
                modifier = Modifier.testTag(OUTER_TAG),
            ) {
                SheetBody()
            }
        }
    }

    private fun setSheet(swipe: Boolean = true, tagged: Boolean = false) {
        compose.setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                MaterialTheme {
                    Box {
                        Text(text = "tick $tick")
                        Sheet(swipe = swipe, tagged = tagged)
                    }
                }
            }
        }
    }

    private fun swipeSheetDown() {
        compose.onNodeWithTag(SHEET_TAG, useUnmergedTree = true).performTouchInput { swipeDown() }
        compose.waitForIdle()
    }

    private fun tapOutside() {
        compose.onRoot().performTouchInput { click(Offset(2f, 2f)) }
        compose.waitForIdle()
    }

    @Test
    fun slidesUpToTheBottomEdge() {
        setSheet()
        compose.onNodeWithText(BODY).assertIsDisplayed()
        val sheet = compose.onNodeWithTag(SHEET_TAG, useUnmergedTree = true).getBoundsInRoot()
        val root = compose.onRoot().getBoundsInRoot()
        // Bottom-aligned: below the middle, never past the root (system bar padding may lift it slightly).
        (sheet.bottom <= root.bottom) shouldBe true
        (sheet.top > root.bottom / 2) shouldBe true
        sheet.height shouldBe 200.dp
        dismissed shouldBe 0
    }

    @Test
    fun swipeDownDismisses() {
        setSheet()
        swipeSheetDown()
        dismissed shouldBe 1
    }

    @Test
    fun tapOutsideDismisses() {
        setSheet()
        tapOutside()
        dismissed shouldBe 1
    }

    @Test
    fun tapOutsideAfterDismissIsNoOp() {
        setSheet()
        swipeSheetDown()
        tapOutside()
        dismissed shouldBe 1
    }

    @Test
    fun tapOnSheetDoesNotDismiss() {
        setSheet()
        compose.onNodeWithText(BODY).performClick()
        compose.waitForIdle()
        dismissed shouldBe 0
    }

    @Test
    fun backDismissesWhileShown() {
        setSheet()
        compose.runOnIdle { owner.pressBack() }
        compose.waitForIdle()
        dismissed shouldBe 1
    }

    @Test
    fun backAfterDismissIsNoOp() {
        setSheet()
        swipeSheetDown()
        compose.runOnIdle { owner.pressBack() }
        compose.waitForIdle()
        dismissed shouldBe 1
    }

    @Test
    fun swipeIgnoredWhenDisabled() {
        setSheet(swipe = false)
        swipeSheetDown()
        dismissed shouldBe 0
        compose.onNodeWithText(BODY).assertIsDisplayed()
        compose.runOnIdle { owner.pressBack() }
        compose.waitForIdle()
        dismissed shouldBe 1
    }

    @Test
    fun usesGivenModifier() {
        setSheet(tagged = true)
        compose.onNodeWithTag(OUTER_TAG).assertIsDisplayed()
        compose.onNodeWithText(BODY).assertIsDisplayed()
    }

    @Test
    fun unchangedRecomposeIsInert() {
        setSheet()
        compose.runOnIdle { tick++ }
        compose.waitForIdle()
        compose.onNodeWithText("tick 1").assertIsDisplayed()
        compose.onNodeWithText(BODY).assertIsDisplayed()
        dismissed shouldBe 0
    }
}
