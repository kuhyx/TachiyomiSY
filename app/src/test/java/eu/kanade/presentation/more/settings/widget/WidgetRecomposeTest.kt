package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import eu.kanade.presentation.more.settings.screen.stubTracker
import eu.kanade.tachiyomi.data.track.Tracker
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Recomposes the preference widgets through hosts that forward their own parameters, first unchanged and
 * then one changed argument at a time, so the compiler's skip and change checks take both arms.
 */
@RunWith(RobolectricTestRunner::class)
internal class WidgetRecomposeTest {
    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var title by mutableStateOf<String?>("Title")
    private var subtitle by mutableStateOf<CharSequence?>("Sub")
    private var icon by mutableStateOf<ImageVector?>(Icons.Outlined.Info)
    private var tint by mutableStateOf(Color.Red)
    private var flag by mutableStateOf(false)
    private var onClick by mutableStateOf<(() -> Unit)?>({})
    private var modifier by mutableStateOf<Modifier>(Modifier)
    private val tracker = stubTracker<Tracker>("Kitsu")

    @Composable
    private fun TextHost(
        modifier: Modifier,
        title: String?,
        subtitle: CharSequence?,
        icon: ImageVector?,
        tint: Color,
        onClick: (() -> Unit)?,
    ) {
        TextPreferenceWidget(modifier, title, subtitle, icon, tint, onClick) { Text("W") }
    }

    @Composable
    private fun SwitchHost(modifier: Modifier, title: String, icon: ImageVector?, checked: Boolean) {
        SwitchPreferenceWidget(modifier, title, subtitle = null, icon = icon, checked = checked) {}
    }

    @Composable
    private fun TrackingHost(modifier: Modifier, loggedIn: Boolean, onClick: (() -> Unit)?) {
        TrackingPreferenceWidget(modifier, tracker, loggedIn, onClick)
    }

    private fun recompose() {
        tick++
        compose.waitForIdle()
    }

    // Unchanged first, then each argument changed on its own.
    private fun cycle() {
        recompose()
        title = "Other"
        recompose()
        subtitle = null
        recompose()
        icon = null
        recompose()
        tint = Color.Blue
        recompose()
        flag = true
        recompose()
        onClick = null
        recompose()
        modifier = Modifier.testTag("changed")
        recompose()
    }

    @Test
    fun hostsForwardParameters() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text("tick $tick")
                    TextHost(modifier, title, subtitle, icon, tint, onClick)
                    SwitchHost(modifier, title.orEmpty(), icon, flag)
                    TrackingHost(modifier, flag, onClick)
                }
            }
        }
        compose.waitForIdle()
        cycle()
        compose.onAllNodesWithText("Other").fetchSemanticsNodes().size shouldBe 2
    }

    @Test
    fun directCallsRecompose() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text("tick $tick")
                    TextPreferenceWidget()
                    TextPreferenceWidget(modifier, title, subtitle, icon, tint, onClick) { Text("W") }
                    SwitchPreferenceWidget(title = title.orEmpty(), checked = flag, onCheckedChanged = {})
                    TrackingPreferenceWidget(tracker = tracker, isLoggedIn = flag)
                    TrackingPreferenceWidget(modifier, tracker, flag, onClick)
                }
            }
        }
        compose.waitForIdle()
        cycle()
        compose.onAllNodesWithText("Other").fetchSemanticsNodes().size shouldBe 2
    }
}
