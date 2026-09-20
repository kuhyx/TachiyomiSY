package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [FastScrollLazyVerticalGrid] composed three ways at once (literal, state-held and host-passed
 * arguments); then every key of the grid scroller's remembered column sums is changed in turn.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
internal class LazyGridMemoTest {
    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var columns: GridCells by mutableStateOf(GridCells.Fixed(2))
    private var padding by mutableStateOf(PaddingValues(0.dp))
    private var horizontal: Arrangement.Horizontal by mutableStateOf(Arrangement.Start)
    private var vertical: Arrangement.Vertical? by mutableStateOf(null)
    private var stateA: LazyGridState? by mutableStateOf(null)
    private var stateB: LazyGridState? by mutableStateOf(null)
    private var thumbColor by mutableStateOf(Color.Unspecified)
    private var edgePadding: Dp by mutableStateOf(0.dp)
    private var isReversed by mutableStateOf(false)
    private var isScrollEnabled by mutableStateOf(true)
    private var gridModifier: Modifier by mutableStateOf(Modifier.height(100.dp))
    private var thumbAllowed by mutableStateOf<() -> Boolean>({ true })
    private var cells by mutableStateOf<LazyGridScope.() -> Unit>({ items(60) { Text(text = "Cell $it") } })

    @Composable
    private fun Host(state: LazyGridState?, columns: GridCells, content: LazyGridScope.() -> Unit) {
        FastScrollLazyVerticalGrid(
            columns = columns,
            modifier = Modifier.height(100.dp),
            state = state,
            thumbAllowed = thumbAllowed,
            thumbColor = thumbColor,
            contentPadding = padding,
            verticalArrangement = vertical,
            horizontalArrangement = horizontal,
            content = content,
        )
    }

    @Test
    fun gridShapes() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    Box(modifier = Modifier.height(100.dp)) {
                        FastScrollLazyVerticalGrid(columns = GridCells.Fixed(2), userScrollEnabled = false) {
                            items(60) { Text(text = "Static $it") }
                        }
                    }
                    FastScrollLazyVerticalGrid(
                        columns = columns,
                        modifier = gridModifier,
                        state = stateA,
                        thumbAllowed = thumbAllowed,
                        thumbColor = thumbColor,
                        contentPadding = padding,
                        topContentPadding = edgePadding,
                        bottomContentPadding = edgePadding,
                        endContentPadding = edgePadding,
                        reverseLayout = isReversed,
                        verticalArrangement = vertical,
                        horizontalArrangement = horizontal,
                        userScrollEnabled = isScrollEnabled,
                        content = cells,
                    )
                    Host(state = stateB, columns = columns, content = cells)
                }
            }
        }
        compose.onNodeWithText("Static 0").assertIsDisplayed()
        val changes: List<() -> Unit> = listOf(
            { tick += 1 },
            { columns = GridCells.Fixed(3) },
            { horizontal = Arrangement.spacedBy(4.dp) },
            { padding = PaddingValues(8.dp) },
            { vertical = Arrangement.spacedBy(2.dp) },
            { stateA = LazyGridState() },
            { stateB = LazyGridState() },
            { thumbColor = Color.Red },
            { edgePadding = 4.dp },
            { isReversed = true },
            { isScrollEnabled = false },
            { gridModifier = Modifier.height(120.dp) },
            { thumbAllowed = { false } },
            { cells = { items(40) { Text(text = "Tile $it") } } },
        )
        changes.forEach { change ->
            compose.runOnIdle(change)
            compose.waitForIdle()
        }
        compose.onAllNodesWithText("Tile 0").onFirst().assertIsDisplayed()
    }
}
