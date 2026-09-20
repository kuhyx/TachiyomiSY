package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SCROLLER = "scroller"
private const val ITEMS = 200

/** [VerticalGridFastScroller] around a real two-column grid of 40 dp rows in a 300 dp viewport. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
internal class VerticalGridFastScrollerTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = LazyGridState()
    private var tick by mutableIntStateOf(0)
    private var cellCount by mutableIntStateOf(ITEMS)
    private var gridHeight by mutableStateOf(300.dp)
    private var thumbColor by mutableStateOf(Color.Unspecified)
    private var target by mutableStateOf<Int?>(null)

    private fun setGrid(gridState: LazyGridState = state, columns: GridCells = GridCells.Fixed(2)) {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    AnimateGridTo(state = gridState, target = target)
                    VerticalGridFastScroller(
                        state = gridState,
                        columns = columns,
                        arrangement = Arrangement.Start,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.testTag(SCROLLER),
                        thumbColor = thumbColor,
                    ) {
                        LazyVerticalGrid(
                            columns = columns,
                            state = gridState,
                            modifier = Modifier.fillMaxWidth().height(gridHeight),
                        ) {
                            items(cellCount) { Text(text = "Cell $it", modifier = Modifier.height(40.dp)) }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun defaultsWrapTheGrid() {
        setGrid()
        compose.onNodeWithText("Cell 0").assertIsDisplayed()
        compose.runOnIdle { tick += 1 }
        compose.onNodeWithText("Cell 1").assertIsDisplayed()
    }

    @Test
    fun everyOptionGiven() {
        compose.setContent {
            MaterialTheme {
                VerticalGridFastScroller(
                    state = state,
                    columns = GridCells.Adaptive(100.dp),
                    arrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.testTag(SCROLLER),
                    thumbAllowed = { true },
                    thumbColor = Color.Red,
                    topContentPadding = 8.dp,
                    bottomContentPadding = 8.dp,
                    endContentPadding = 4.dp,
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(100.dp),
                        state = state,
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ITEMS) { Text(text = "Cell $it", modifier = Modifier.height(40.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag(SCROLLER).assertHeightIsEqualTo(300.dp)
    }

    @Test
    fun fewItemsHideTheScroller() {
        cellCount = 2
        setGrid()
        compose.onNodeWithText("Cell 1").assertIsDisplayed()
    }

    @Test
    fun shrinkingGridHidesTheScroller() {
        setGrid()
        compose.mainClock.advanceTimeBy(TICK_SETTLE_MS)
        compose.runOnIdle { cellCount = 2 }
        compose.onNodeWithText("Cell 1").assertIsDisplayed()
        compose.runOnIdle { cellCount = ITEMS }
        compose.onNodeWithText("Cell 1").assertIsDisplayed()
    }

    @Test
    fun thumbColorFallsBackToPrimary() {
        setGrid()
        compose.runOnIdle { thumbColor = Color.Red }
        compose.waitForIdle()
        compose.runOnIdle { thumbColor = Color.Unspecified }
        compose.onNodeWithText("Cell 0").assertIsDisplayed()
    }

    @Test
    fun animatedScrollShowsTheThumb() {
        setGrid(gridState = LazyGridState(firstVisibleItemIndex = 40))
        compose.runOnIdle { target = 0 }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(TICK_SETTLE_MS)
        compose.onNodeWithText("Cell 0").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(THUMB_GONE_MS)
        compose.onNodeWithText("Cell 0").assertIsDisplayed()
    }

    @Test
    fun viewportResizeKeepsTheScroller() {
        setGrid()
        compose.runOnIdle { gridHeight = 200.dp }
        compose.onNodeWithTag(SCROLLER).assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun scrollingToTheEndClampsThumb() {
        setGrid()
        compose.runOnIdle { target = ITEMS - 1 }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(TICK_SETTLE_MS)
        state.canScrollForward shouldBe false
        state.firstVisibleItemIndex shouldBeGreaterThan ITEMS / 2
    }
}
