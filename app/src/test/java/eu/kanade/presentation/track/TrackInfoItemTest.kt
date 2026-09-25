package eu.kanade.presentation.track

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.presentation.track.components.TrackLogoIconPreviewProvider
import eu.kanade.presentation.track.components.TrackLogoIconPreviews
import eu.kanade.presentation.util.tapOutsidePopup
import eu.kanade.test.DummyTracker
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TrackInfoItemTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val tracker = DummyTracker(id = 1L, name = "Dummy")

    @Test
    fun menuTogglesPrivateOn() {
        compose.setContent {
            MaterialTheme {
                TrackInfoItemMenu(
                    onOpenInBrowser = {},
                    onRemoved = {},
                    onCopyLink = {},
                    private = false,
                    onTogglePrivate = { events += "private" },
                )
            }
        }
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Track privately").performClick()
        compose.onNodeWithContentDescription("More").performClick()
        compose.tapOutsidePopup()
        compose.onNodeWithText("Track privately").assertDoesNotExist()
        events shouldContainExactly listOf("private")
    }

    @Test
    fun titleLongPressCopies() {
        compose.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Row { TrackTitle(title = "Copied", onNewSearch = {}) }
            }
        }
        compose.onNodeWithText("Copied").performTouchInput { longClick() }
        compose.waitForIdle()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.primaryClip?.getItemAt(0)?.text shouldBe "Copied"
    }

    @Test
    fun logoLongPressAndDefaults() {
        compose.setContent {
            MaterialTheme {
                TrackLogoIcon(tracker = tracker, onClick = { events += "click" }, onLongClick = { events += "long" })
                TrackLogoIcon(tracker = tracker.copy(name = "Plain"))
            }
        }
        compose.onNodeWithContentDescription("Dummy").performTouchInput { longClick() }
        compose.onNodeWithContentDescription("Plain").assertExists()
        events shouldContainExactly listOf("long")
    }

    @Test
    fun logoPreviewRenders() {
        val trackers = TrackLogoIconPreviewProvider().values.toList()
        compose.setContent { trackers.forEach { TrackLogoIconPreviews(it) } }
        compose.onNodeWithContentDescription("Dummy Tracker").assertExists()
    }
}
