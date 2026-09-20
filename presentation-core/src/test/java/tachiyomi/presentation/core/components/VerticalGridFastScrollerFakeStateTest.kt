package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

private const val SCROLLER = "scroller"
private const val ROW_PX = 100

/**
 * A grid state that reports a hand-built layout, so the grid can be emptied under a thumb that is
 * already on screen: neither the drag nor the scroll effect may then move a list that has no items.
 */
@RunWith(RobolectricTestRunner::class)
internal class VerticalGridFastScrollerFakeStateTest {
    @get:Rule
    val compose = createComposeRule()

    private val layout = FakeLazyGridLayoutInfo(gridRows(rows = 3, rowHeight = ROW_PX), 20)
    private val scrollOffset = mutableIntStateOf(0)
    private val state = mockk<LazyGridState>(relaxed = true)

    private fun setScroller() {
        every { state.layoutInfo } returns layout.info
        every { state.firstVisibleItemScrollOffset } answers { scrollOffset.intValue }
        compose.setContent {
            MaterialTheme {
                VerticalGridFastScroller(
                    state = state,
                    columns = GridCells.Fixed(2),
                    arrangement = Arrangement.Start,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.testTag(SCROLLER),
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) { Text(text = "content") }
                }
            }
        }
    }

    @Test
    fun emptiedGridStopsFollowingThumb() {
        setScroller()
        // A scroll-offset change relaunches the effect, which ticks the thumb into view.
        compose.runOnIdle { scrollOffset.intValue = 5 }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(TICK_SETTLE_MS)
        compose.runOnIdle { layout.totalItemsCount = 0 }
        compose.onNodeWithTag(SCROLLER).performTouchInput {
            down(Offset(width - 6.dp.toPx(), 24.dp.toPx()))
            moveBy(Offset(0f, 60f))
        }
        compose.waitForIdle()
        compose.runOnIdle { scrollOffset.intValue = 7 }
        compose.waitForIdle()
        compose.onNodeWithTag(SCROLLER).performTouchInput { up() }
        compose.waitForIdle()
        compose.onNodeWithText("content").assertIsDisplayed()
        coVerify(exactly = 0) { state.scrollToItem(any(), any()) }
    }

    @Test
    fun scrollOffsetChangeMovesThumb() {
        setScroller()
        compose.runOnIdle { scrollOffset.intValue = 50 }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(TICK_SETTLE_MS)
        compose.onNodeWithText("content").assertIsDisplayed()
        coVerify(exactly = 0) { state.scrollToItem(any(), any()) }
    }
}
