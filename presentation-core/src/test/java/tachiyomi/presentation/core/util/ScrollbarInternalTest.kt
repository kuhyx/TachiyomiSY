package tachiyomi.presentation.core.util

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Drives the internal `drawScrollbar` with every argument shape the compiler distinguishes. */
@RunWith(RobolectricTestRunner::class)
internal class ScrollbarInternalTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val tracer = ComposeTracerRule()

    private var tick by mutableIntStateOf(0)
    private var orientation by mutableStateOf(Orientation.Vertical)
    private var state by mutableStateOf(LazyListState())
    private var reverse by mutableStateOf(false)
    private var offset by mutableStateOf(0f)

    @Composable
    private fun Host(orientation: Orientation, state: LazyListState, reverse: Boolean, offset: Float) {
        ScrolledList(Modifier.drawScrollbar(state, orientation, reverse, offset), state)
    }

    @Composable
    private fun ScrolledList(modifier: Modifier, state: LazyListState) {
        LazyColumn(modifier = modifier.height(100.dp), state = state) {
            items(30) { Text(text = "Row $it") }
        }
    }

    @Test
    fun everyArgumentShapeRecomposes() {
        compose.setContent {
            MaterialTheme {
                Text(text = "tick $tick")
                ScrolledList(Modifier.drawScrollbar(LazyListState(), Orientation.Vertical, false, 0f), state)
                ScrolledList(Modifier.drawScrollbar(state, orientation, reverse, offset), state)
                Host(orientation = orientation, state = state, reverse = reverse, offset = offset)
            }
        }
        compose.onNodeWithText("tick 0").assertIsDisplayed()
        val changes: List<() -> Unit> = listOf(
            { tick += 1 },
            { orientation = Orientation.Horizontal },
            { state = LazyListState() },
            { reverse = true },
            { offset = 4f },
            { tick += 1 },
        )
        changes.forEach { change ->
            compose.runOnIdle(change)
            compose.waitForIdle()
        }
        compose.onNodeWithText("tick 2").assertIsDisplayed()
        tracer.started shouldBeGreaterThan 0
    }
}
