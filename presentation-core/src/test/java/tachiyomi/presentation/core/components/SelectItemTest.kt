package tachiyomi.presentation.core.components

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val Fruits = arrayOf("Apple", "Banana", "Cherry")

@RunWith(RobolectricTestRunner::class)
internal class SelectItemTest {
    @get:Rule
    val compose = createComposeRule()

    private var selected by mutableIntStateOf(0)

    private fun setSelectItem() {
        compose.setContent {
            MaterialTheme {
                SelectItem(
                    label = "Fruit",
                    options = Fruits,
                    selectedIndex = selected,
                    onSelect = { index -> selected = index },
                )
            }
        }
    }

    private fun sendOutsideTouchToPopupWindow() {
        val popupRoot = compose.onNode(isPopup()).fetchSemanticsNode().root as ViewRootForTest
        val popupWindow = popupRoot.view.parent as View
        compose.runOnUiThread {
            val now = SystemClock.uptimeMillis()
            val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_OUTSIDE, 0f, 0f, 0)
            popupWindow.onTouchEvent(event)
            event.recycle()
        }
        compose.waitForIdle()
    }

    @Test
    fun showsSelectedOptionCollapsed() {
        setSelectItem()
        compose.onNodeWithText("Apple").assertIsDisplayed()
        compose.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun pickingAnOptionSelectsIt() {
        setSelectItem()
        compose.onNodeWithText("Fruit").performClick()
        compose.onNode(isPopup()).assertExists()
        compose.onNodeWithText("Banana").performClick()
        compose.runOnIdle { selected shouldBe 1 }
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.onNodeWithText("Banana").assertIsDisplayed()
    }

    @Test
    fun anchorTapTogglesTheMenu() {
        setSelectItem()
        compose.onNodeWithText("Fruit").performClick()
        compose.onNode(isPopup()).assertExists()
        compose.onNodeWithText("Fruit").performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { selected shouldBe 0 }
    }

    @Test
    fun outsideTapDismissesTheMenu() {
        setSelectItem()
        compose.onNodeWithText("Fruit").performClick()
        compose.onNode(isPopup()).assertExists()
        sendOutsideTouchToPopupWindow()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { selected shouldBe 0 }
    }
}
