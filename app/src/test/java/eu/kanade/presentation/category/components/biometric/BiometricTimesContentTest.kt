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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.util.CallWithUnstableBits
import eu.kanade.presentation.util.invokeClick
import eu.kanade.presentation.util.recomposeAll
import eu.kanade.tachiyomi.ui.category.biometric.TimeRange
import eu.kanade.tachiyomi.ui.category.biometric.TimeRangeItem
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.hours

private const val FACADE = "eu.kanade.presentation.category.components.biometric.BiometricTimesContentKt"

private fun range(hour: Int) = TimeRange(hour.hours, (hour + 1).hours).let { TimeRangeItem(it, it.toString()) }

/**
 * Every arm of the memoized list content: the same and new arguments read from state and passed
 * through a parameterised host, and a caller carrying the unstable-stability bit.
 */
@RunWith(RobolectricTestRunner::class)
internal class BiometricTimesContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val tick = mutableIntStateOf(0)
    private var ranges by mutableStateOf(listOf(range(1)))
    private val deleted = mutableListOf<TimeRangeItem>()
    private var onDelete by mutableStateOf<(TimeRangeItem) -> Unit>({ deleted += it })

    @Composable
    private fun Host(ranges: List<TimeRangeItem>, onDelete: (TimeRangeItem) -> Unit, tick: Int) {
        Text(text = "host $tick")
        BiometricTimesContent(ranges, LazyListState(), PaddingValues(), onDelete)
    }

    @Test
    fun recomposesWithNewArguments() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick ${tick.intValue}")
                    BiometricTimesContent(ranges, LazyListState(), PaddingValues(), onDelete)
                    Host(ranges = ranges, onDelete = onDelete, tick = tick.intValue)
                    CallWithUnstableBits(
                        owner = Class.forName(FACADE),
                        name = "BiometricTimesContent",
                        args = listOf(ranges, LazyListState(), PaddingValues(), onDelete),
                    )
                }
            }
        }
        compose.recomposeAll(tick, { ranges = listOf(range(2)) }, { onDelete = { deleted += it } })
        compose.onNodeWithText("tick 3").assertExists()
    }

    @Test
    fun deleteReportsTheRange() {
        compose.setContent {
            MaterialTheme {
                BiometricTimesContent(ranges, LazyListState(), PaddingValues(), onDelete)
            }
        }
        compose.onAllNodes(hasClickAction()).onFirst().invokeClick()
        compose.runOnIdle { deleted shouldContainExactly listOf(range(1)) }
    }
}
