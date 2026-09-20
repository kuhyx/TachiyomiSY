package tachiyomi.presentation.core.components

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.presentation.core.util.ComposeTracerRule

private fun sheetState(): AnchoredDraggableState<Int> = AnchoredDraggableState(
    initialValue = 0,
    anchors = DraggableAnchors {
        0 at 0f
        1 at 500f
    },
)

/** Drives the internal bottom-sheet helpers with every argument shape the compiler distinguishes. */
@RunWith(RobolectricTestRunner::class)
internal class BottomSheetHelpersTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val tracer = ComposeTracerRule()

    private var tick by mutableIntStateOf(0)
    private var state by mutableStateOf(sheetState())
    private var dismissed = 0
    private var onDismiss: () -> Unit by mutableStateOf<() -> Unit>({ dismissed++ })
    private var swipe by mutableStateOf(true)

    @Composable
    private fun DismissHost(state: AnchoredDraggableState<Int>, onDismiss: () -> Unit) {
        ShowThenDismissWhenHidden(state, onDismiss)
    }

    @Composable
    private fun ScrollHost(state: AnchoredDraggableState<Int>, swipe: Boolean, scope: CoroutineScope) {
        Box(modifier = Modifier.size(10.dp).nestedScrollToSheetWhen(state, swipe, scope))
    }

    @Test
    fun dismissHelperRecomposes() {
        compose.setContent {
            MaterialTheme {
                Text(text = "tick $tick")
                ShowThenDismissWhenHidden(remember { sheetState() }) {}
                ShowThenDismissWhenHidden(state, onDismiss)
                DismissHost(state = state, onDismiss = onDismiss)
            }
        }
        compose.onNodeWithText("tick 0").assertIsDisplayed()
        val changes: List<() -> Unit> = listOf(
            { tick += 1 },
            { onDismiss = { dismissed += 2 } },
            { state = sheetState() },
            { tick += 1 },
        )
        changes.forEach { change ->
            compose.runOnIdle(change)
            compose.waitForIdle()
        }
        compose.onNodeWithText("tick 2").assertIsDisplayed()
        dismissed shouldBe 0
        tracer.started shouldBeGreaterThan 0
    }

    @Test
    fun scrollHelperRecomposes() {
        compose.setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                Text(text = "tick $tick")
                Box(modifier = Modifier.size(10.dp).nestedScrollToSheetWhen(remember { sheetState() }, true, scope))
                Box(modifier = Modifier.size(10.dp).nestedScrollToSheetWhen(state, swipe, scope))
                ScrollHost(state = state, swipe = swipe, scope = scope)
            }
        }
        compose.onNodeWithText("tick 0").assertIsDisplayed()
        val changes: List<() -> Unit> = listOf(
            { tick += 1 },
            { state = sheetState() },
            { swipe = false },
            { swipe = true },
            { tick += 1 },
        )
        changes.forEach { change ->
            compose.runOnIdle(change)
            compose.waitForIdle()
        }
        compose.onNodeWithText("tick 2").assertIsDisplayed()
    }
}
