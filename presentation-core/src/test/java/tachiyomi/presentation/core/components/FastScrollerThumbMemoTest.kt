package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val LAYOUT = "layout"

/**
 * [rememberThumbAlpha] and [FastScrollerLayout] composed three ways at once (literal, state-held and
 * host-passed arguments); then every captured instance is swapped in turn.
 */
@RunWith(RobolectricTestRunner::class)
internal class FastScrollerThumbMemoTest {
    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var ticks by mutableStateOf(newTicks())
    private var thumbAllowed by mutableStateOf<() -> Boolean>({ true })
    private var content by mutableStateOf<@Composable () -> Unit>({ Box(modifier = Modifier.size(50.dp)) })
    private var scroller by mutableStateOf<@Composable (Int, Constraints) -> Unit>({ _, _ -> Text(text = "bar") })
    private var allowedCalls = 0

    private fun newTicks(): MutableSharedFlow<Unit> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @Composable
    private fun AlphaHost(scrolled: MutableSharedFlow<Unit>, thumbAllowed: () -> Boolean) {
        rememberThumbAlpha(scrolled, thumbAllowed)
    }

    @Composable
    private fun LayoutHost(content: @Composable () -> Unit, scroller: @Composable (Int, Constraints) -> Unit) {
        FastScrollerLayout(modifier = Modifier.testTag(LAYOUT), content = content, scroller = scroller)
    }

    @Test
    fun alphaShapes() {
        compose.setContent {
            Column {
                Text(text = "tick $tick")
                rememberThumbAlpha(rememberScrolledTicks()) { true }
                rememberThumbAlpha(ticks, thumbAllowed)
                AlphaHost(scrolled = ticks, thumbAllowed = thumbAllowed)
            }
        }
        compose.runOnIdle { tick += 1 }
        compose.waitForIdle()
        compose.runOnIdle {
            thumbAllowed = {
                allowedCalls += 1
                false
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { ticks = newTicks() }
        compose.waitForIdle()
        compose.runOnIdle { ticks.tryEmit(Unit) shouldBe true }
        compose.mainClock.advanceTimeBy(TICK_SETTLE_MS)
        allowedCalls shouldBe 2
    }

    @Test
    fun layoutShapes() {
        compose.setContent {
            Column {
                Text(text = "tick $tick")
                FastScrollerLayout(modifier = Modifier, content = { Text(text = "literal") }) { _, _ ->
                    Text(text = "bar")
                }
                FastScrollerLayout(modifier = Modifier, content = content, scroller = scroller)
                LayoutHost(content = content, scroller = scroller)
            }
        }
        compose.onNodeWithText("literal").assertIsDisplayed()
        compose.runOnIdle { tick += 1 }
        compose.waitForIdle()
        compose.runOnIdle { content = { Text(text = "swapped") } }
        compose.waitForIdle()
        compose.runOnIdle { scroller = { _, _ -> Box(modifier = Modifier.size(4.dp)) } }
        compose.waitForIdle()
        compose.onNodeWithTag(LAYOUT).assertIsDisplayed()
    }
}
