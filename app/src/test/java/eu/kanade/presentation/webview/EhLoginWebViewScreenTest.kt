package eu.kanade.presentation.webview

import android.content.Context
import android.content.pm.ApplicationInfo
import android.webkit.WebView
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.kevinnzou.web.AccompanistWebViewClient
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class EhLoginWebViewScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun show() {
        compose.setContent {
            MaterialTheme {
                EhLoginWebViewScreen(
                    onUp = { events += "up" },
                    onPageFinished = { _, url -> events += "finished $url" },
                    onClickRecheckLoginStatus = { load ->
                        events += "recheck"
                        load("https://recheck.example/")
                    },
                    onClickAlternateLoginPage = { events += "alternate" },
                    onClickSkipPageRestyling = { events += "skip" },
                    onClickCustomIgneousCookie = { events += "igneous" },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun webView(): WebView = compose.webViews().single()

    @Test
    fun pagesReportOnlyWithAUrl() {
        show()
        compose.onNodeWithText("ExHentai login").assertExists()
        val client = shadowOf(webView()).webViewClient as AccompanistWebViewClient
        compose.runOnIdle {
            client.onPageFinished(webView(), null)
            client.onPageFinished(webView(), "https://done.example/")
        }
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf("finished https://done.example/", "up", "up")
    }

    @Test
    fun advancedOptionsCloseDialog() {
        val info = ApplicationProvider.getApplicationContext<Context>().applicationInfo
        info.flags = info.flags or ApplicationInfo.FLAG_DEBUGGABLE
        show()
        listOf(
            "Recheck login status",
            "Alternative login page",
            "Skip page restyling",
            "Custom igneous cookie",
            "Cancel",
        ).forEach {
            compose.onNodeWithText("Advanced").performClick()
            clickLast(it)
        }
        events shouldContainExactly listOf("recheck", "alternate", "skip", "igneous")
    }

    /** The dialog's button is the last node with [text] (the screen behind has its own "Cancel"). */
    private fun clickLast(text: String) {
        val nodes = compose.onAllNodes(hasText(text) and hasClickAction())
        nodes[nodes.fetchSemanticsNodes().size - 1].performClick()
        compose.waitForIdle()
    }
}
