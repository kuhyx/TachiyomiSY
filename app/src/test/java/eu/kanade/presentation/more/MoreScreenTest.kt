package eu.kanade.presentation.more

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.Constants

@RunWith(RobolectricTestRunner::class)
internal class MoreScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val uriHandler = mockk<UriHandler>(relaxed = true)

    private fun show(state: DownloadQueueState, navTabs: Boolean) {
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                MaterialTheme {
                    MoreScreen(
                        downloadQueueStateProvider = { state },
                        downloadedOnly = false,
                        onDownloadedOnlyChange = { events += "downloaded:$it" },
                        incognitoMode = true,
                        onIncognitoModeChange = { events += "incognito:$it" },
                        showNavUpdates = navTabs,
                        showNavHistory = navTabs,
                        onClickDownloadQueue = { events += "queue" },
                        onClickCategories = { events += "categories" },
                        onClickStats = { events += "stats" },
                        onClickDataAndStorage = { events += "data" },
                        onClickSettings = { events += "settings" },
                        onClickAbout = { events += "about" },
                        onClickBatchAdd = { events += "batch" },
                        onClickUpdates = { events += "updates" },
                        onClickHistory = { events += "history" },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performClick()
    }

    private fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun everyRowReports() {
        show(DownloadQueueState.Stopped, navTabs = false)
        listOf(
            "Downloaded only", "Incognito mode", "Updates", "History", "Download queue", "Categories",
            "Statistics", "Data and storage", "Batch Add", "Settings", "About", "Help",
        ).forEach(::tap)
        events shouldBe listOf(
            "downloaded:true", "incognito:false", "updates", "history", "queue", "categories",
            "stats", "data", "batch", "settings", "about",
        )
        verify { uriHandler.openUri(Constants.URL_HELP) }
    }

    @Test
    fun navTabsHideLinks() {
        show(DownloadQueueState.Paused(pending = 0), navTabs = true)
        count("Updates") shouldBe 0
        count("History") shouldBe 0
        count("Paused") shouldBe 1
    }

    @Test
    fun pausedWithPending() {
        show(DownloadQueueState.Paused(pending = 3), navTabs = true)
        count("Paused • 3 remaining") shouldBe 1
    }

    @Test
    fun downloadingShowsPending() {
        show(DownloadQueueState.Downloading(pending = 1), navTabs = true)
        count("1 remaining") shouldBe 1
    }

    @Test
    fun newUpdatePreview() {
        compose.setContent { NewUpdateScreenPreview() }
        compose.onNodeWithText("New version available!").assertExists()
        compose.onNodeWithText("Open on GitHub").performClick()
        compose.onNodeWithText("Not now").performClick()
        compose.onNodeWithText("Download").performClick()
    }
}
