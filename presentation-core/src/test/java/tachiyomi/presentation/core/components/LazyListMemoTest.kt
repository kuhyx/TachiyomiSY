package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The same wrapper composed three ways at once (literal arguments, state-held arguments, and a host
 * composable that re-passes changed parameters), then every state argument is changed in turn.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
internal class LazyListMemoTest {
    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var padding by mutableStateOf(PaddingValues(0.dp))
    private var isScrollEnabled by mutableStateOf(true)
    private var isReversed by mutableStateOf(false)
    private var alignment: Alignment.Horizontal by mutableStateOf(Alignment.Start)
    private var listModifier: Modifier by mutableStateOf(Modifier.height(100.dp))
    private var stateA: LazyListState? by mutableStateOf(null)
    private var stateB: LazyListState? by mutableStateOf(null)
    private var arrangement: Arrangement.Vertical? by mutableStateOf(null)
    private var rows by mutableStateOf<LazyListScope.() -> Unit>({ items(30) { Text(text = "Row $it") } })

    @Composable
    private fun ScrollbarHost(
        state: LazyListState?,
        arrangement: Arrangement.Vertical?,
        content: LazyListScope.() -> Unit,
    ) {
        ScrollbarLazyColumn(
            modifier = Modifier.height(100.dp),
            state = state,
            contentPadding = padding,
            verticalArrangement = arrangement,
            userScrollEnabled = isScrollEnabled,
            content = content,
        )
    }

    @Composable
    private fun FastHost(
        state: LazyListState?,
        arrangement: Arrangement.Vertical?,
        content: LazyListScope.() -> Unit,
    ) {
        FastScrollLazyColumn(
            modifier = Modifier.height(100.dp),
            state = state,
            contentPadding = padding,
            verticalArrangement = arrangement,
            userScrollEnabled = isScrollEnabled,
            content = content,
        )
    }

    private fun changeEverything() {
        compose.runOnIdle { tick += 1 }
        compose.waitForIdle()
        compose.runOnIdle { padding = PaddingValues(8.dp) }
        compose.waitForIdle()
        compose.runOnIdle { isScrollEnabled = false }
        compose.waitForIdle()
        compose.runOnIdle { isReversed = true }
        compose.waitForIdle()
        compose.runOnIdle { alignment = Alignment.CenterHorizontally }
        compose.waitForIdle()
        compose.runOnIdle { listModifier = Modifier.height(120.dp) }
        compose.waitForIdle()
        compose.runOnIdle { stateA = LazyListState() }
        compose.waitForIdle()
        compose.runOnIdle { stateB = LazyListState() }
        compose.waitForIdle()
        compose.runOnIdle { arrangement = Arrangement.spacedBy(2.dp) }
        compose.waitForIdle()
        compose.runOnIdle { rows = { items(20) { Text(text = "Line $it") } } }
        compose.waitForIdle()
        compose.onAllNodesWithText("Line 0").onFirst().assertIsDisplayed()
    }

    @Test
    fun scrollbarColumnShapes() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    Box(modifier = Modifier.height(100.dp)) {
                        ScrollbarLazyColumn(contentPadding = PaddingValues(2.dp), userScrollEnabled = false) {
                            items(30) { Text(text = "Static $it") }
                        }
                    }
                    ScrollbarLazyColumn(
                        modifier = listModifier,
                        state = stateA,
                        contentPadding = padding,
                        reverseLayout = isReversed,
                        verticalArrangement = arrangement,
                        horizontalAlignment = alignment,
                        userScrollEnabled = isScrollEnabled,
                        content = rows,
                    )
                    ScrollbarHost(state = stateB, arrangement = arrangement, content = rows)
                }
            }
        }
        compose.onNodeWithText("Static 0").assertIsDisplayed()
        changeEverything()
    }

    @Test
    fun fastScrollColumnShapes() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    Box(modifier = Modifier.height(100.dp)) {
                        FastScrollLazyColumn(contentPadding = PaddingValues(2.dp), userScrollEnabled = false) {
                            items(30) { Text(text = "Static $it") }
                        }
                    }
                    FastScrollLazyColumn(
                        modifier = listModifier,
                        state = stateA,
                        contentPadding = padding,
                        reverseLayout = isReversed,
                        verticalArrangement = arrangement,
                        horizontalAlignment = alignment,
                        userScrollEnabled = isScrollEnabled,
                        content = rows,
                    )
                    FastHost(state = stateB, arrangement = arrangement, content = rows)
                }
            }
        }
        compose.onNodeWithText("Static 0").assertIsDisplayed()
        changeEverything()
    }
}
