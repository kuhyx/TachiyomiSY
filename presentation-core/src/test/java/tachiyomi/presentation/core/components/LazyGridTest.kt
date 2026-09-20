package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val GRID = "grid"
private const val ITEMS = 60

/** [FastScrollLazyVerticalGrid]: a LazyVerticalGrid inside the grid fast scroller, defaults filled in. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
internal class LazyGridTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = LazyGridState()
    private var tick by mutableIntStateOf(0)
    private var isScrollEnabled by mutableStateOf(true)

    private val cells: LazyGridScope.() -> Unit = {
        items(ITEMS) { Text(text = "Cell $it", modifier = Modifier.height(40.dp)) }
    }

    @Test
    fun defaults() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    FastScrollLazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(300.dp),
                        content = cells,
                    )
                }
            }
        }
        compose.onNodeWithText("Cell 0").assertIsDisplayed()
        compose.runOnIdle { tick += 1 }
        compose.onNodeWithText("Cell 1").assertIsDisplayed()
    }

    @Test
    fun everyOption() {
        compose.setContent {
            MaterialTheme {
                FastScrollLazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().height(300.dp).testTag(GRID),
                    state = state,
                    thumbAllowed = { true },
                    thumbColor = Color.Red,
                    contentPadding = PaddingValues(8.dp),
                    topContentPadding = 8.dp,
                    bottomContentPadding = 8.dp,
                    endContentPadding = 4.dp,
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    userScrollEnabled = isScrollEnabled,
                    content = cells,
                )
            }
        }
        compose.onNodeWithTag(GRID).assertHeightIsEqualTo(300.dp)
        compose.runOnIdle { isScrollEnabled = false }
        compose.onNodeWithTag(GRID).assertHeightIsEqualTo(300.dp)
        state.layoutInfo.reverseLayout shouldBe true
    }

    @Test
    fun reversedWithoutArrangement() {
        compose.setContent {
            MaterialTheme {
                FastScrollLazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(300.dp),
                    reverseLayout = true,
                    content = cells,
                )
            }
        }
        compose.onNodeWithText("Cell 0").assertIsDisplayed()
    }
}
