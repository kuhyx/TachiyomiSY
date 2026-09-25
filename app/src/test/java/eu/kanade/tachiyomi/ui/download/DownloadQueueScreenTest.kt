package eu.kanade.tachiyomi.ui.download

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.clearQueue
import eu.kanade.tachiyomi.data.download.pauseDownloads
import eu.kanade.tachiyomi.data.download.reorderQueue
import eu.kanade.tachiyomi.data.download.startDownloads
import eu.kanade.tachiyomi.ui.base.ScreenHost
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class DownloadQueueScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = DownloadHarness()
    private val first = download(1)

    @Before
    fun setUp() {
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerQueueKt")
        every { harness.manager.startDownloads() } just runs
        every { harness.manager.pauseDownloads() } just runs
        every { harness.manager.clearQueue() } just runs
        every { harness.manager.reorderQueue(any()) } just runs
        every { harness.manager.statusFlow() } returns flowOf(first)
        every { harness.manager.progressFlow() } returns flowOf(first)
        startKoin { modules(module { single { harness.manager } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    private fun show() {
        // The queue list inflates Material views, which need the app theme MainActivity carries.
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
        compose.setContent { ScreenHost(DownloadQueueScreen) }
        compose.waitForIdle()
    }

    private fun sortBy(group: String, order: String) {
        compose.onNodeWithContentDescription("Sort").performClick()
        compose.onNodeWithText(group).performClick()
        compose.onNodeWithText(order).performClick()
        compose.waitForIdle()
    }

    @Test
    fun emptyQueueSaysSo() {
        show()
        compose.onNodeWithText("No downloads").assertExists()
        compose.onNodeWithText("Download queue").assertExists()
    }

    @Test
    fun resumeStartsTheDownloader() {
        harness.queue.value = listOf(first, download(2))
        show()
        compose.onNodeWithText("2").assertExists()
        compose.onNodeWithText("Resume", useUnmergedTree = true).performClick()
        verify { harness.manager.startDownloads() }
        harness.running.value = true
        compose.waitForIdle()
        compose.onNodeWithText("Pause", useUnmergedTree = true).performClick()
        verify { harness.manager.pauseDownloads() }
    }

    @Test
    fun sortMenuReordersTheQueue() {
        harness.queue.value = listOf(first, download(2))
        show()
        sortBy("By upload date", "Newest")
        sortBy("By upload date", "Oldest")
        sortBy("By chapter number", "Ascending")
        sortBy("By chapter number", "Descending")
        verify(exactly = 4) { harness.manager.reorderQueue(any()) }
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Cancel all").performClick()
        verify { harness.manager.clearQueue() }
    }

    @Test
    @Config(qualifiers = "night")
    fun darkThemeCountsToo() {
        harness.queue.value = (1L..30L).map { download(it) }
        show()
        compose.onNodeWithText("30").assertExists()
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onRoot().performTouchInput { swipeDown() }
        compose.onRoot().performTouchInput { swipeDown() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Navigate up").performClick()
    }
}
