package eu.kanade.tachiyomi.ui.manga.track

import android.content.ClipboardManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.domainTrack
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class TrackHomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = TrackHomeHarness()
    private val tracker get() = harness.tracker
    private val track = domainTrack(trackerId = 1L, score = 5.0, remoteUrl = "https://t.example/1")
        .copy(status = 1L, lastChapterRead = 3.0, totalChapters = 5L, startDate = 1L, finishDate = 1L)

    @Before
    fun setUp() {
        harness.start()
        every { tracker.getLogo() } returns R.drawable.brand_anilist
        every { tracker.getStatus(any()) } returns MR.strings.reading
        every { tracker.getStatusList() } returns listOf(1L)
        every { tracker.getScoreList() } returns listOf("5")
        every { tracker.displayScore(any()) } returns "5"
        every { tracker.supportsReadingDates } returns true
        every { tracker.supportsPrivateTracking } returns true
        harness.tracks.value = listOf(track)
    }

    @After
    fun tearDown() = harness.stop()

    private fun show() {
        compose.setContent {
            MaterialTheme { Navigator(listOf(BlankScreen(), TrackInfoDialogHomeScreen(1L, "Needle", 7L))) }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("Title")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun menu(item: String) {
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText(item).performClick()
        compose.waitForIdle()
    }

    @Test
    fun detailsOpenSelectors() {
        show()
        compose.onNodeWithText("Reading").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Status").assertExists()
    }

    @Test
    fun chaptersOpenTheirSelector() {
        show()
        compose.onNodeWithText("3 / 5").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Chapters").assertExists()
    }

    @Test
    fun scoreOpensItsSelector() {
        show()
        compose.onNodeWithText("5").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Score").assertExists()
    }

    @Test
    fun titleStartsANewSearch() {
        show()
        compose.onNodeWithText("Title").performClick()
        compose.waitForIdle()
        coVerify(timeout = 5_000) { tracker.search("Title") }
    }

    @Test
    fun menuOpensTheBrowser() {
        show()
        menu("Open in browser")
        shadowOf(harness.app).nextStartedActivity?.dataString shouldBe "https://t.example/1"
    }

    @Test
    fun menuCopiesTheLink() {
        show()
        menu("Copy link")
        val clipboard = harness.app.getSystemService(ClipboardManager::class.java)
        clipboard.primaryClip?.getItemAt(0)?.text shouldBe "https://t.example/1"
    }

    @Test
    fun menuTogglesPrivacyAndRemoves() {
        show()
        menu("Make private")
        coVerify(timeout = 5_000) { tracker.setRemotePrivate(any(), true) }
        menu("Remove")
        compose.onNodeWithText("Remove Plain tracking?").assertExists()
    }

    @Test
    fun enhancedTrackersRegister() {
        val enhanced = mockk<BaseTracker>(relaxed = true, moreInterfaces = arrayOf(EnhancedTracker::class))
        every { enhanced.id } returns 2L
        every { enhanced.name } returns "Enhanced"
        every { enhanced.getLogo() } returns R.drawable.brand_anilist
        every { (enhanced as EnhancedTracker).accept(any()) } returns true
        coEvery { harness.getManga.await(1L) } returns null
        every { harness.trackerManager.loggedInTrackers() } returns listOf(tracker, enhanced)
        show()
        compose.onNodeWithText("Add tracking").performClick()
        coVerify(timeout = 5_000) { harness.getManga.await(1L) }
    }

    @Test
    fun loadingShowsAPlaceholder() {
        compose.setContent { MaterialTheme { LoadingPlaceholder() } }
        compose.onNodeWithText("Loading…").assertExists()
    }
}
