package eu.kanade.tachiyomi.ui.updates

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import eu.kanade.tachiyomi.ui.base.TabHost
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class UpdatesTabTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = UpdatesHarness()

    @Before
    fun setUp() {
        startKoin { modules(harness.koinModules() + module { single { UiPreferences(harness.store) } }) }
        mockkStatic("eu.kanade.tachiyomi.data.library.LibraryUpdateSchedulingKt")
        every { LibraryUpdateJob.startNow(any<Context>()) } returns false
        val now = System.currentTimeMillis()
        harness.updates.value = listOf(update(1, dateFetch = now), update(2, dateFetch = now))
        coEvery { harness.getChapter.await(any()) } returns null
        coEvery { harness.getManga.await(any()) } returns null
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
        HomeScreen.showBottomNavEvent.tryReceive()
    }

    private fun node(label: String) =
        compose.onNode(hasText(label) or hasContentDescription(label), useUnmergedTree = true)

    private fun waitFor(text: String) =
        compose.waitUntil(WAIT) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    // App-bar actions that do not fit the narrow test screen sit in the overflow menu.
    private fun action(label: String) {
        val visible = compose.onAllNodes(hasText(label) or hasContentDescription(label), useUnmergedTree = true)
        if (visible.fetchSemanticsNodes().isEmpty()) node("More options").performClick()
        compose.waitForIdle()
        node(label).performClick()
        compose.waitForIdle()
    }

    private fun show() {
        compose.setContent { TabHost(UpdatesTab) }
        compose.waitUntil(WAIT) { compose.onAllNodes(hasText("C1")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun openingAChapterStartsTheReader() {
        show()
        compose.onNodeWithText("C1").performClick()
        compose.waitForIdle()
        shadowOf(compose.activity).nextStartedActivity.component?.className shouldBe ReaderActivity::class.java.name
    }

    @Test
    fun appBarActions() {
        show()
        node("Update library").performClick()
        waitFor("An update is already running")
        node("View Upcoming Updates").performClick()
        waitFor("opened:UpcomingScreen")
    }

    @Test
    fun filterDialogOpens() {
        show()
        node("Filter").performClick()
        compose.waitForIdle()
        node("Downloaded").performClick()
        compose.waitForIdle()
        harness.updatesPreferences.filterDownloaded.get().name shouldBe "ENABLED_IS"
    }

    @Test
    fun selectionActions() {
        show()
        compose.onNodeWithText("C1").performTouchInput { longClick() }
        compose.waitForIdle()
        // One of two rows selected: inverting selects the other, then everything is selected.
        listOf("Select inverse", "Select all", "Mark as read").forEach(::action)
        compose.waitForIdle()
        coVerify(timeout = WAIT) { harness.getChapter.await(1L) }
        coVerify(timeout = WAIT) { harness.getChapter.await(2L) }
    }

    @Test
    fun tabOptionsAndReselect() {
        show()
        node("tab:Updates").assertExists()
        val navigator = mockk<Navigator>(relaxed = true)
        runBlocking { UpdatesTab.onReselect(navigator) }
        verify { navigator.push(DownloadQueueScreen) }
    }

    @Test
    fun enabledFollowsThePreference() {
        var enabled: Boolean? = null
        compose.setContent { enabled = UpdatesTab.isEnabled() }
        compose.waitForIdle()
        enabled shouldBe true
    }

    @Test
    fun deletingDownloadedChapters() {
        every { harness.downloadManager.isChapterDownloaded(any(), any(), "/c/1", any(), any(), any()) } returns true
        show()
        compose.onNodeWithText("C1").performTouchInput { longClick() }
        compose.waitForIdle()
        // Selecting hides the home bottom bar; receiving the signal lets the effect finish.
        compose.waitUntil(WAIT) { HomeScreen.showBottomNavEvent.tryReceive().isSuccess }
        action("Delete")
        node("OK").performClick()
        compose.waitForIdle()
        coVerify(timeout = WAIT) { harness.getManga.await(1L) }
    }
}

private const val WAIT = 5_000L
