package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.presentation.core.util.ComposeTracerRule

/** Drives `rememberColumnWidthSums` with every argument shape the compiler distinguishes. */
@RunWith(RobolectricTestRunner::class)
internal class ColumnWidthSumsTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val tracer = ComposeTracerRule()

    private var tick by mutableIntStateOf(0)
    private var columns by mutableStateOf<GridCells>(GridCells.Fixed(2))
    private var arrangement by mutableStateOf(Arrangement.Start)
    private var padding by mutableStateOf(PaddingValues(0.dp))
    private var sums = emptyList<Int>()

    @Composable
    private fun Host(columns: GridCells, arrangement: Arrangement.Horizontal, padding: PaddingValues) {
        val density = LocalDensity.current
        val sizes = rememberColumnWidthSums(columns, arrangement, padding)
        sums = density.sizes(Constraints(maxWidth = 200))
    }

    @Test
    fun everyArgumentShapeRecomposes() {
        compose.setContent {
            MaterialTheme {
                Text(text = "tick $tick")
                rememberColumnWidthSums(GridCells.Fixed(4), Arrangement.Start, PaddingValues(0.dp))
                rememberColumnWidthSums(columns, arrangement, padding)
                Host(columns = columns, arrangement = arrangement, padding = padding)
            }
        }
        compose.onNodeWithText("tick 0").assertIsDisplayed()
        sums shouldBe listOf(100, 200)
        val changes: List<() -> Unit> = listOf(
            { tick += 1 },
            { columns = GridCells.Fixed(4) },
            { arrangement = Arrangement.spacedBy(8.dp) },
            { padding = PaddingValues(10.dp) },
            { tick += 1 },
        )
        changes.forEach { change ->
            compose.runOnIdle(change)
            compose.waitForIdle()
        }
        compose.onNodeWithText("tick 2").assertIsDisplayed()
        sums.size shouldBe 4
    }

    @Test
    fun sumsAreCumulative() {
        var result = emptyList<Int>()
        compose.setContent {
            val sizes = rememberColumnWidthSums(GridCells.Fixed(3), Arrangement.Start, PaddingValues(0.dp))
            result = Density(1f).sizes(Constraints(maxWidth = 300))
        }
        result shouldBe listOf(100, 200, 300)
    }
}
