package eu.kanade.presentation.more.settings.widget

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.more.settings.LocalPreferenceHighlighted
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.Tracker
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BasePreferenceWidgetTest {
    @get:Rule
    val compose = createComposeRule()

    private fun tracker(user: String): Tracker = mockk {
        every { name } returns "Tracker X"
        every { getLogo() } returns R.drawable.ic_tachi
        every { getDisplayUsername() } returns user
    }

    private fun loggedIn(): Int = compose.onAllNodesWithContentDescription("Logged in").fetchSemanticsNodes().size

    @Test
    fun highlightBlinksThenFades() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalPreferenceHighlighted provides true) {
                    BasePreferenceWidget(title = " ", icon = { Text("Icon") }, widget = { Text("End") })
                }
            }
        }
        compose.mainClock.advanceTimeBy(4_000)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText("End").assertExists()
        compose.onNodeWithText("Icon").assertExists()
    }

    @Test
    fun iconAndClickableText() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                TextPreferenceWidget(title = "Tap", icon = Icons.Filled.Preview, onPreferenceClick = { clicks++ })
            }
        }
        compose.onNodeWithText("Tap").performClick()
        clicks shouldBe 1
    }

    @Test
    fun trackerLoggedInShowsUser() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                TrackingPreferenceWidget(tracker = tracker("someone"), isLoggedIn = true, onClick = { clicks++ })
            }
        }
        compose.onNodeWithText("someone").assertExists()
        loggedIn() shouldBe 1
        compose.onNodeWithText("Tracker X").performClick()
        clicks shouldBe 1
    }

    @Test
    fun trackerBlankUserHidden() {
        compose.setContent {
            MaterialTheme { TrackingPreferenceWidget(tracker = tracker(" "), isLoggedIn = true) }
        }
        compose.onNodeWithText("Tracker X").performClick()
        loggedIn() shouldBe 1
    }

    @Test
    fun trackerLoggedOutHidesUser() {
        compose.setContent {
            MaterialTheme { TrackingPreferenceWidget(tracker = tracker("someone"), isLoggedIn = false) }
        }
        compose.onAllNodesWithContentDescription("someone").fetchSemanticsNodes().size shouldBe 0
        loggedIn() shouldBe 0
    }
}
