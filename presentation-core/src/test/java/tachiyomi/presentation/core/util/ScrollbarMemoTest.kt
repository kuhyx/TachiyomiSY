package tachiyomi.presentation.core.util

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val ITEMS = 60

/**
 * The scrollbar modifiers composed three ways at once (literal, state-held and host-passed arguments);
 * then the list state, the reverse flag and the offset are changed one at a time.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
internal class ScrollbarMemoTest {
    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var stateA by mutableStateOf(LazyListState())
    private var stateB by mutableStateOf(LazyListState())
    private var isReversed by mutableStateOf(false)
    private var offset by mutableFloatStateOf(0f)
    private var host: View? = null

    @Composable
    private fun Rows(state: LazyListState, modifier: Modifier) {
        LazyColumn(modifier = modifier.fillMaxWidth().height(100.dp), state = state) {
            items(ITEMS) { Text(text = "Row $it", modifier = Modifier.height(40.dp)) }
        }
    }

    @Composable
    private fun Cells(state: LazyListState, modifier: Modifier) {
        LazyRow(modifier = modifier.fillMaxWidth().height(50.dp), state = state) {
            items(ITEMS) { Text(text = "Cell $it", modifier = Modifier.width(60.dp).height(40.dp)) }
        }
    }

    @Composable
    private fun VerticalHost(state: LazyListState, reverse: Boolean, offset: Float) {
        Rows(state = state, modifier = Modifier.drawVerticalScrollbar(state, reverse, offset))
    }

    @Composable
    private fun HorizontalHost(state: LazyListState, reverse: Boolean, offset: Float) {
        Cells(state = state, modifier = Modifier.drawHorizontalScrollbar(state, reverse, offset))
    }

    private fun changeEverything(text: String) {
        compose.forceDraw(checkNotNull(host))
        compose.runOnIdle { tick += 1 }
        compose.waitForIdle()
        compose.forceDraw(checkNotNull(host))
        compose.runOnIdle { isReversed = true }
        compose.waitForIdle()
        compose.runOnIdle { offset = 6f }
        compose.waitForIdle()
        compose.runOnIdle { stateA = LazyListState() }
        compose.waitForIdle()
        compose.runOnIdle { stateB = LazyListState() }
        compose.waitForIdle()
        compose.forceDraw(checkNotNull(host))
        compose.onAllNodesWithText(text).onFirst().assertIsDisplayed()
    }

    @Test
    fun verticalShapes() {
        val literal = LazyListState()
        compose.setContent {
            host = LocalView.current
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    Rows(state = literal, modifier = Modifier.drawVerticalScrollbar(literal, false, 2f))
                    Rows(state = stateA, modifier = Modifier.drawVerticalScrollbar(stateA, isReversed, offset))
                    VerticalHost(state = stateB, reverse = isReversed, offset = offset)
                }
            }
        }
        changeEverything("Row 0")
    }

    @Test
    fun horizontalShapes() {
        val literal = LazyListState()
        compose.setContent {
            host = LocalView.current
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    Cells(state = literal, modifier = Modifier.drawHorizontalScrollbar(literal, false, 2f))
                    Cells(state = stateA, modifier = Modifier.drawHorizontalScrollbar(stateA, isReversed, offset))
                    HorizontalHost(state = stateB, reverse = isReversed, offset = offset)
                }
            }
        }
        changeEverything("Cell 0")
    }
}
