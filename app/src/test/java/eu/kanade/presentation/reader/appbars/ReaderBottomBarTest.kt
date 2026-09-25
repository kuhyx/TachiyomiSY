package eu.kanade.presentation.reader.appbars

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ReaderBottomBarTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(callbacks: ReaderBarCallbacks, sy: SyBottomBarState, crop: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                ReaderBottomBar(
                    settings = callbacks.settings(crop),
                    onClickSettings = { callbacks.events += "settings" },
                    sy = sy,
                    syActions = callbacks.actions(),
                )
            }
        }
        compose.waitForIdle()
    }

    private fun click(description: String) = compose.onNodeWithContentDescription(description).performClick()

    @Test
    fun everyButtonForwards() {
        val callbacks = ReaderBarCallbacks()
        show(callbacks, syState())
        listOf(
            "Chapters",
            "Open in WebView",
            "Open in browser",
            "Share",
            "Reading mode",
            "Default rotation",
            "Crop borders",
            "Page layout",
            "Shift one page over",
            "Settings",
        ).forEach(::click)
        callbacks.events shouldContainExactly listOf(
            "chapters",
            "webview",
            "browser",
            "share",
            "mode",
            "orientation",
            "crop",
            "layout",
            "shift",
            "settings",
        )
    }

    @Test
    fun missingLinksAndDisabledButtons() {
        show(ReaderBarCallbacks(withLinks = false), syState(buttons = emptySet(), doublePages = false), crop = false)
        compose.onNodeWithContentDescription("Open in WebView").assertDoesNotExist()
        compose.onNodeWithContentDescription("Chapters").assertDoesNotExist()
        compose.onNodeWithContentDescription("Crop borders").assertDoesNotExist()
        compose.onNodeWithContentDescription("Shift one page over").assertDoesNotExist()
    }

    @Test
    fun linksWithoutCallbacksHide() {
        show(ReaderBarCallbacks(withLinks = false), syState(split = true), crop = false)
        compose.onNodeWithContentDescription("Share").assertDoesNotExist()
        compose.onNodeWithContentDescription("Page layout").assertDoesNotExist()
        click("Crop borders")
    }

    @Test
    fun webtoonUsesItsCropButton() {
        val callbacks = ReaderBarCallbacks()
        show(callbacks, syState(mode = ReadingMode.WEBTOON))
        compose.onNodeWithContentDescription("Page layout").assertDoesNotExist()
        click("Crop borders")
        callbacks.events shouldContainExactly listOf("crop")
    }

    @Test
    fun continuousVerticalUsesItsCropButton() {
        val callbacks = ReaderBarCallbacks()
        show(callbacks, syState(mode = ReadingMode.CONTINUOUS_VERTICAL))
        click("Crop borders")
        callbacks.events shouldContainExactly listOf("crop")
    }
}
