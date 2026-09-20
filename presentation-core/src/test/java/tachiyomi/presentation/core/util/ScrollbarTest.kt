package tachiyomi.presentation.core.util

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val LIST = "list"
private const val ITEMS = 100
private const val SCROLLBAR_GONE_MS = 300L + 250L + 100L

/** The scrollbar modifiers on real lazy lists: a touch scroll ticks the bar in, and it fades back out. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
internal class ScrollbarTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = LazyListState()
    private var isReversed by mutableStateOf(false)
    private var host: View? = null
    private var draws = 0

    @Composable
    private fun Rows(modifier: Modifier) {
        LazyColumn(modifier = modifier.fillMaxWidth().height(300.dp).testTag(LIST), state = state) {
            items(ITEMS) { Text(text = "Row $it", modifier = Modifier.height(40.dp)) }
        }
    }

    @Composable
    private fun Cells(modifier: Modifier) {
        LazyRow(modifier = modifier.fillMaxWidth().height(60.dp).testTag(LIST), state = state) {
            items(ITEMS) { Text(text = "Cell $it", modifier = Modifier.width(60.dp).height(40.dp)) }
        }
    }

    private fun setDirected(direction: LayoutDirection, content: @Composable () -> Unit) {
        compose.setContent {
            host = LocalView.current
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Box(
                        modifier = Modifier.drawWithContent {
                            drawContent()
                            draws += 1
                        },
                    ) {
                        content()
                    }
                }
            }
        }
        forceDrawAndCount()
    }

    private fun forceDrawAndCount(): Int {
        compose.forceDraw(checkNotNull(host))
        draws shouldBeGreaterThan 0
        return draws
    }

    private fun flipReverse() {
        val before = draws
        compose.runOnIdle { isReversed = !isReversed }
        forceDrawAndCount() shouldBeGreaterThan before
    }

    @Test
    fun verticalTicksOnSwipeAndFades() {
        setDirected(LayoutDirection.Ltr) { Rows(modifier = Modifier.drawVerticalScrollbar(state)) }
        compose.onNodeWithTag(LIST).performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.runOnIdle { state.firstVisibleItemIndex shouldBeGreaterThan 0 }
        forceDrawAndCount()
        compose.mainClock.advanceTimeBy(SCROLLBAR_GONE_MS)
        forceDrawAndCount()
        compose.onNodeWithTag(LIST).performTouchInput { swipeUp() }
        compose.waitForIdle()
        forceDrawAndCount()
    }

    @Test
    fun verticalRtlWithEveryOption() {
        setDirected(LayoutDirection.Rtl) {
            Rows(
                modifier = Modifier.drawVerticalScrollbar(state, reverseScrolling = isReversed, positionOffsetPx = 6f),
            )
        }
        flipReverse()
        flipReverse()
    }

    @Test
    fun horizontalTicksInOnASwipe() {
        setDirected(LayoutDirection.Ltr) { Cells(modifier = Modifier.drawHorizontalScrollbar(state)) }
        compose.onNodeWithTag(LIST).performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.runOnIdle { state.firstVisibleItemIndex shouldBeGreaterThan 0 }
        forceDrawAndCount()
        compose.mainClock.advanceTimeBy(SCROLLBAR_GONE_MS)
        forceDrawAndCount()
    }

    @Test
    fun horizontalRtlWithEveryOption() {
        setDirected(LayoutDirection.Rtl) {
            Cells(
                modifier = Modifier.drawHorizontalScrollbar(
                    state,
                    reverseScrolling = isReversed,
                    positionOffsetPx = 6f,
                ),
            )
        }
        flipReverse()
        flipReverse()
    }

    @Test
    fun horizontalLtrReversed() {
        setDirected(LayoutDirection.Ltr) {
            Cells(modifier = Modifier.drawHorizontalScrollbar(state, reverseScrolling = true))
        }
        forceDrawAndCount()
    }
}
