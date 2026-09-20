package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.presentation.core.util.FakeLazyListLayoutInfo
import tachiyomi.presentation.core.util.uniformItems

private const val SCROLLER = "scroller"
private const val ITEM_PX = 100

/**
 * A list state that reports whatever layout the test says: the corners a real list never shows
 * (visible items with a zero count; a list that empties under a thumb already on screen).
 */
@RunWith(RobolectricTestRunner::class)
internal class VerticalFastScrollerFakeStateTest {
    @get:Rule
    val compose = createComposeRule()

    private var isScrolling = false
    private var thumbColor by mutableStateOf(Color.Unspecified)

    private fun fakeState(layout: FakeLazyListLayoutInfo): LazyListState {
        val state = mockk<LazyListState>(relaxed = true)
        every { state.layoutInfo } returns layout
        every { state.isScrollInProgress } answers { isScrolling }
        return state
    }

    private fun setScroller(state: LazyListState) {
        compose.setContent {
            MaterialTheme {
                VerticalFastScroller(
                    listState = state,
                    modifier = Modifier.testTag(SCROLLER),
                    thumbColor = thumbColor,
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) { Text(text = "content") }
                }
            }
        }
    }

    @Test
    fun zeroCountWithItemsShowsNoThumb() {
        val state = fakeState(FakeLazyListLayoutInfo(uniformItems(count = 4, size = ITEM_PX), 0))
        setScroller(state)
        compose.onNodeWithText("content").assertIsDisplayed()
        coVerify(exactly = 0) { state.scrollToItem(any(), any()) }
    }

    @Test
    fun emptiedListStopsFollowingThumb() {
        val layout = FakeLazyListLayoutInfo(uniformItems(count = 4, size = ITEM_PX), 20)
        val state = fakeState(layout)
        setScroller(state)
        // A scroll in progress ticks the thumb into view.
        isScrolling = true
        compose.runOnIdle { thumbColor = Color.Red }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(TICK_SETTLE_MS)
        // The scroll ends as the list empties; the tracker still reports the previous frame's scroll.
        isScrolling = false
        layout.totalItemsCount = 0
        compose.runOnIdle { thumbColor = Color.Blue }
        compose.waitForIdle()
        compose.onNodeWithTag(SCROLLER).performTouchInput {
            down(Offset(width - 14.dp.toPx(), 24.dp.toPx()))
            moveBy(Offset(0f, 60f))
            up()
        }
        compose.waitForIdle()
        compose.onNodeWithText("content").assertIsDisplayed()
        coVerify(exactly = 0) { state.scrollToItem(any(), any()) }
    }
}
