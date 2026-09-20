package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
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
private const val START_INDEX = 20

/**
 * Dragging the list's thumb: the thumb sits at the end edge, 12 dp wide inside 8 dp of horizontal padding,
 * 48 dp tall, and at the top of its track once the list has animated back to item 0.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
internal class VerticalFastScrollerDragTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = LazyListState(firstVisibleItemIndex = START_INDEX)
    private var target by mutableStateOf<Int?>(null)

    private fun setList() {
        compose.setContent {
            MaterialTheme {
                AnimateListTo(state = state, target = target)
                VerticalFastScroller(listState = state, modifier = Modifier.testTag(SCROLLER)) {
                    LazyColumn(state = state, modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        items(ITEMS) { Text(text = "Item $it", modifier = Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }

    private fun animateToTopAndSettleThumb() {
        compose.runOnIdle { target = 0 }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(TICK_SETTLE_MS)
        state.firstVisibleItemIndex shouldBe 0
    }

    @Test
    fun draggingTheThumbScrollsTheList() {
        setList()
        animateToTopAndSettleThumb()
        compose.onNodeWithTag(SCROLLER).performTouchInput {
            down(Offset(width - 14.dp.toPx(), 24.dp.toPx()))
            moveBy(Offset(0f, 100f))
        }
        compose.waitForIdle()
        compose.runOnIdle { state.firstVisibleItemIndex shouldBeGreaterThan 0 }
        compose.onNodeWithTag(SCROLLER).performTouchInput {
            moveBy(Offset(0f, 60f))
        }
        compose.waitForIdle()
        compose.onNodeWithTag(SCROLLER).performTouchInput { moveBy(Offset(0f, -600f)) }
        compose.waitForIdle()
        compose.runOnIdle { state.firstVisibleItemIndex shouldBe 0 }
        compose.onNodeWithTag(SCROLLER).performTouchInput { up() }
        compose.waitForIdle()
        compose.runOnIdle { state.firstVisibleItemIndex shouldBe 0 }
        compose.mainClock.advanceTimeBy(THUMB_GONE_MS)
        state.firstVisibleItemIndex shouldBe 0
    }

    @Test
    fun aFadedThumbCannotBeDragged() {
        setList()
        animateToTopAndSettleThumb()
        compose.mainClock.advanceTimeBy(THUMB_GONE_MS)
        compose.onNodeWithTag(SCROLLER).performTouchInput {
            down(Offset(width - 14.dp.toPx(), 24.dp.toPx()))
            moveBy(Offset(0f, 100f))
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle { state.firstVisibleItemIndex shouldBe 0 }
    }
}
