package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val BODY = "Dialog body"
private const val DIALOG_TAG = "dialog"

/** The tablet half of [AdaptiveSheet]: a centred dialog that fades in. */
@RunWith(RobolectricTestRunner::class)
internal class AdaptiveSheetDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val owner = SheetBackOwner()
    private var dismissed = 0
    private var tick by mutableIntStateOf(0)

    @Composable
    private fun Sheet(tagged: Boolean) {
        if (!tagged) {
            AdaptiveSheet(
                isTabletUi = true,
                enableSwipeDismiss = true,
                onDismissRequest = { dismissed++ },
            ) {
                Text(text = BODY)
            }
        } else {
            AdaptiveSheet(
                isTabletUi = true,
                enableSwipeDismiss = true,
                onDismissRequest = { dismissed++ },
                modifier = Modifier.testTag(DIALOG_TAG),
            ) {
                Text(text = BODY)
            }
        }
    }

    private fun setSheet(tagged: Boolean = false) {
        compose.setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                MaterialTheme {
                    // The sheet's full-size scrim is drawn over the tick label, so a corner tap hits the scrim.
                    Box {
                        Text(text = "tick $tick")
                        Sheet(tagged = tagged)
                    }
                }
            }
        }
    }

    @Test
    fun fadesInAndShowsContent() {
        setSheet()
        compose.onNodeWithText(BODY).assertIsDisplayed()
        dismissed shouldBe 0
    }

    @Test
    fun tapOutsideDismisses() {
        setSheet()
        compose.onRoot().performTouchInput { click(Offset(2f, 2f)) }
        compose.waitForIdle()
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
    fun backDismissesOnceShown() {
        setSheet()
        compose.runOnIdle { owner.pressBack() }
        compose.waitForIdle()
        dismissed shouldBe 1
    }

    @Test
    fun backIgnoredBeforeFadeIn() {
        compose.mainClock.autoAdvance = false
        setSheet()
        compose.runOnIdle { owner.pressBack() }
        dismissed shouldBe 0
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle { owner.pressBack() }
        compose.waitForIdle()
        dismissed shouldBe 1
    }

    @Test
    fun usesGivenModifier() {
        setSheet(tagged = true)
        compose.onNodeWithTag(DIALOG_TAG).assertIsDisplayed()
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
