package eu.kanade.presentation.category.components.biometric

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.util.recomposeAll
import eu.kanade.tachiyomi.ui.category.biometric.TimeRange
import eu.kanade.tachiyomi.ui.category.biometric.TimeRangeItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.hours

@RunWith(RobolectricTestRunner::class)
internal class BiometricTimesContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val tick = mutableIntStateOf(0)
    private var ranges by mutableStateOf(listOf(range(1)))
    private var onDelete by mutableStateOf<(TimeRangeItem) -> Unit>({})
    private val listState = LazyListState()

    private fun range(hour: Int) = TimeRange(hour.hours, (hour + 1).hours).let { TimeRangeItem(it, it.toString()) }

    @Composable
    private fun Host(ranges: List<TimeRangeItem>, onDelete: (TimeRangeItem) -> Unit, tick: Int) {
        Text(text = "host $tick")
        BiometricTimesContent(ranges, LazyListState(), PaddingValues(), onDelete)
    }

    @Composable
    private fun <L : List<TimeRangeItem>> BoundHost(ranges: L, onDelete: (TimeRangeItem) -> Unit) {
        BiometricTimesContent(ranges, LazyListState(), PaddingValues(), onDelete)
    }

    @Test
    fun recomposesWithNewArguments() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick ${tick.intValue}")
                    BiometricTimesContent(ranges, listState, PaddingValues(), onDelete)
                    Host(ranges = ranges, onDelete = onDelete, tick = tick.intValue)
                    BoundHost(ranges = ranges, onDelete = onDelete)
                }
            }
        }
        compose.recomposeAll(tick, { ranges = listOf(range(2)) }, { onDelete = {} })
        compose.onNodeWithText("tick 3").assertExists()
    }
}
