package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.core.stack.StackEvent
import eu.kanade.tachiyomi.ui.browse.TabHost
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

/** Coming back to the feed from a pushed screen, and rows refreshed from the library. */
@RunWith(RobolectricTestRunner::class)
internal class FeedTabMoreTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = FeedHarness()

    @Before
    fun setUp() {
        harness.start()
        every { harness.getManga.subscribe(any(), any()) } returns flowOf(null)
        coEvery { harness.getSavedSearches.await() } returns listOf(savedSearch(5L))
        harness.feeds.value = listOf(feed(1L), feed(2L, savedSearch = 5L))
    }

    @After
    fun tearDown() = harness.stop()

    private fun host(label: String = "Latest"): TabHost = TabHost { feedTab() }.also { host ->
        host.show(compose)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText(label)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Popping back skips the reload while the pop is in flight, then reloads once the navigator settles.
    @Test
    fun popBackReloadsOnce() {
        val host = host()
        compose.onNodeWithText("Search 5").performClick()
        compose.waitForIdle()
        compose.runOnUiThread { host.navigator.pop() }
        compose.waitForIdle()
        host.navigator.lastEvent shouldBe StackEvent.Idle
        // Loaded when the model starts and on the first idle composition, then once after the pop settles.
        // The reload runs on the main looper, which a blocking verify would starve: poll and idle instead.
        eventually { runCatching { coVerify(exactly = 3) { harness.source.getLatestUpdates(1) } }.isSuccess }
    }

    @Test
    fun libraryCopyReplacesRow() {
        every { harness.getManga.subscribe(any(), any()) } answers {
            flowOf(null, Manga.create().copy(id = 4L, url = firstArg(), ogTitle = "Library copy", source = 1L))
        }
        host("Library copy")
    }

    @Test
    fun stateReportsEmptiness() {
        FeedScreenState().isEmpty shouldBe true
        FeedScreenState(items = emptyList()).isEmpty shouldBe true
        FeedScreenState(items = listOf(mockk())).isEmpty shouldBe false
        FeedScreenState().isLoading shouldBe true
    }
}
