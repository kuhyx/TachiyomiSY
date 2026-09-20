package tachiyomi.presentation.core.components

import androidx.compose.foundation.lazy.LazyItemScope
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val PickerSize = DpSize(120.dp, 120.dp)

/** The internal wheel with its row content and items passed every way the compiler distinguishes. */
@RunWith(RobolectricTestRunner::class)
internal class WheelPickerContentTest {

    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var items by mutableStateOf(listOf(1, 2, 3))
    private var row: @Composable LazyItemScope.(Int) -> Unit by mutableStateOf({ Text(text = "row $it") })

    @Composable
    private fun Host(items: List<Int>, row: @Composable LazyItemScope.(Int) -> Unit) {
        WheelPicker(
            items = items,
            modifier = Modifier,
            startIndex = 0,
            size = PickerSize,
            onSelectionChanged = {},
            backgroundContent = null,
            itemContent = row,
        )
    }

    @Test
    fun rowContentAndItemsRecompose() {
        compose.setContent {
            MaterialTheme {
                Text(text = "tick $tick")
                Host(items = items, row = row)
            }
        }
        compose.onNodeWithText("row 1").assertIsDisplayed()
        val changes: List<() -> Unit> = listOf(
            { tick += 1 },
            { row = { Text(text = "cell $it") } },
            { items = listOf(7, 8, 9) },
            { tick += 1 },
        )
        changes.forEach { change ->
            compose.runOnIdle(change)
            compose.waitForIdle()
        }
        compose.onNodeWithText("cell 7").assertIsDisplayed()
    }
}
