package tachiyomi.presentation.core.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val EXPECTED_MESSAGE = "LazyVerticalGrid's width should be bound by parent"

/**
 * The scroller refuses an unbounded width: the grid itself is given a fixed width so it lays out
 * and reports items, while the scroller's own constraints come from a horizontally scrolling row.
 */
@RunWith(RobolectricTestRunner::class)
internal class VerticalGridFastScrollerWidthTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = LazyGridState()

    @Test
    fun unboundedWidthIsRejected() {
        val error = shouldThrowAny {
            compose.setContent {
                MaterialTheme {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        VerticalGridFastScroller(
                            state = state,
                            columns = GridCells.Fixed(2),
                            arrangement = Arrangement.Start,
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = state,
                                modifier = Modifier.width(200.dp).height(300.dp),
                            ) {
                                items(100) { Text(text = "Cell $it", modifier = Modifier.height(40.dp)) }
                            }
                        }
                    }
                }
            }
            compose.waitForIdle()
        }
        val causes = generateSequence(error) { it.cause }
        causes.any { it is IllegalArgumentException && it.message == EXPECTED_MESSAGE } shouldBe true
    }
}
