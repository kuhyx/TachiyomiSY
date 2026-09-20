package tachiyomi.presentation.core.components.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The elevation object handed to a [Button] every way the compiler distinguishes. */
@RunWith(RobolectricTestRunner::class)
internal class ButtonElevationHostTest {

    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var elevation by mutableStateOf(ButtonDefaults.buttonElevation())
    private var enabled by mutableStateOf(true)

    @Composable
    private fun Host(elevation: ButtonElevation, enabled: Boolean) {
        Button(onClick = {}, enabled = enabled, elevation = elevation) { Text(text = "hosted") }
    }

    @Test
    fun elevationInstancesRecompose() {
        compose.setContent {
            MaterialTheme {
                Text(text = "tick $tick")
                Button(onClick = {}, elevation = ButtonDefaults.flatElevation()) { Text(text = "flat") }
                Button(onClick = {}, elevation = elevation, enabled = enabled) { Text(text = "held") }
                Host(elevation = elevation, enabled = enabled)
            }
        }
        compose.onNodeWithText("hosted").assertIsDisplayed()
        val changes: List<() -> Unit> = listOf(
            { tick += 1 },
            { elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp) },
            { enabled = false },
            { enabled = true },
            { elevation = ButtonDefaults.flatElevation() },
            { tick += 1 },
        )
        changes.forEach { change ->
            compose.runOnIdle(change)
            compose.waitForIdle()
        }
        compose.onNodeWithText("tick 2").assertIsDisplayed()
    }
}
