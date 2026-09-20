package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The memoization arms of [ScaffoldLayout]'s measure lambda: the same slots on a plain
 * recomposition, new slot instances, and slots handed down as changed parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
internal class ScaffoldLayoutRememberTest {
    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var fabPosition by mutableStateOf(FabPosition.End)
    private var insets by mutableStateOf(harnessInsets)
    private var topBar by mutableStateOf<@Composable () -> Unit>({})
    private var startBar by mutableStateOf<@Composable () -> Unit>({})
    private var snackbar by mutableStateOf<@Composable () -> Unit>({})
    private var fab by mutableStateOf<@Composable () -> Unit>({})
    private var bottomBar by mutableStateOf<@Composable () -> Unit>({})
    private var body by mutableStateOf<@Composable (PaddingValues) -> Unit>({ Text("body") })

    @Composable
    private fun Host(
        fabPosition: FabPosition,
        insets: WindowInsets,
        topBar: @Composable () -> Unit,
        startBar: @Composable () -> Unit,
        snackbar: @Composable () -> Unit,
        fab: @Composable () -> Unit,
        bottomBar: @Composable () -> Unit,
        body: @Composable (PaddingValues) -> Unit,
    ) {
        ScaffoldLayout(
            fabPosition = fabPosition,
            topBar = topBar,
            startBar = startBar,
            content = body,
            snackbar = snackbar,
            fab = fab,
            contentWindowInsets = insets,
            bottomBar = bottomBar,
        )
    }

    private fun recompose() {
        tick += 1
        compose.waitForIdle()
    }

    private fun replaceEachSlotInTurn() {
        fabPosition = FabPosition.Center
        recompose()
        insets = WindowInsets(left = 2.dp, top = 2.dp, right = 2.dp, bottom = 2.dp)
        recompose()
        topBar = { BottomBar() }
        recompose()
        startBar = { TaggedBox(tag = "start", width = START_BAR_WIDTH, height = null) }
        recompose()
        snackbar = { TaggedBox(tag = "snackbar", width = SNACKBAR_WIDTH, height = SNACKBAR_HEIGHT) }
        recompose()
        fab = { Fab() }
        recompose()
        bottomBar = { BottomBar() }
        recompose()
        body = { Text("new body") }
        recompose()
        compose.onNodeWithTag("fab").assertExists()
        compose.onNodeWithText("new body").assertExists()
    }

    @Test
    fun sameSlotsThenNewSlots() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    Text("tick $tick")
                    Box(Modifier.fillMaxSize()) {
                        ScaffoldLayout(
                            fabPosition = fabPosition,
                            topBar = topBar,
                            startBar = startBar,
                            content = body,
                            snackbar = snackbar,
                            fab = fab,
                            contentWindowInsets = insets,
                            bottomBar = bottomBar,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("body").assertExists()
        recompose()
        compose.onNodeWithText("body").assertExists()
        replaceEachSlotInTurn()
        recompose()
    }

    @Test
    fun slotsAsChangedParameters() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    Text("tick $tick")
                    Box(Modifier.fillMaxSize()) {
                        Host(
                            fabPosition = fabPosition,
                            insets = insets,
                            topBar = topBar,
                            startBar = startBar,
                            snackbar = snackbar,
                            fab = fab,
                            bottomBar = bottomBar,
                            body = body,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        recompose()
        compose.onNodeWithText("body").assertExists()
        replaceEachSlotInTurn()
        recompose()
        fabPosition = FabPosition.End
        compose.waitForIdle()
    }
}
