package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.tachiyomi.ui.browse.TabHost
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.sy.SYMR

@RunWith(RobolectricTestRunner::class)
internal class FeedTabTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = FeedHarness()

    @Before
    fun setUp() {
        harness.start()
        every { harness.source.toString() } returns "One (EN)"
        every { harness.getManga.subscribe(any(), any()) } returns flowOf(null)
        coEvery { harness.getSavedSearches.await() } returns listOf(savedSearch(5L))
        harness.feeds.value = listOf(feed(1L), feed(2L, savedSearch = 5L))
    }

    @After
    fun tearDown() = harness.stop()

    private fun host(): TabHost = TabHost { feedTab() }.also { host ->
        host.show(compose)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("Latest")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun headersOpenTheSource() {
        val host = host()
        host.content.titleRes shouldBe SYMR.strings.feed
        compose.onNodeWithText("One").performClick()
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<BrowseSourceScreen>()
        harness.koin.sourcePreferences.lastUsedSource.get() shouldBe 1L
    }

    @Test
    fun savedSearchHeaderOpensTheSearch() {
        val host = host()
        compose.onNodeWithText("Search 5").performClick()
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<BrowseSourceScreen>()
    }

    @Test
    fun mangaOpensTheEntry() {
        val host = host()
        compose.onNodeWithText("Latest").performClick()
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<MangaScreen>()
    }

    @Test
    fun longClickDeletesTheFeed() {
        host()
        compose.onNodeWithText("One").performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("Delete")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Delete").performClick()
        coVerify(timeout = 5_000) { harness.delete.await(1L) }
    }

    @Test
    fun addingPicksSourceThenSearch() {
        val host = host()
        host.action("Add")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("One (EN)")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("One (EN)").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("Latest")).fetchSemanticsNodes().size > 1
        }
        compose.onAllNodes(hasText("Latest"))[1].performClick()
        compose.onNodeWithText("OK").performClick()
        coVerify(timeout = 5_000) { harness.insert.await(feed(-1L)) }
    }

    @Test
    fun addingWithoutChoiceCloses() {
        val host = host()
        host.action("Add")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("OK")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("OK").assertDoesNotExist()
    }

    @Test
    fun tooManyFeedsWarns() {
        coEvery { harness.count.await() } returns 11L
        val host = host()
        host.action("Add")
        compose.waitUntil(timeoutMillis = 5_000) { host.snackbar.currentSnackbarData != null }
    }

    @Test
    fun failuresWarn() {
        every { harness.getFeeds.subscribe() } returns flow { error("db") }
        val host = TabHost { feedTab() }
        host.show(compose)
        compose.waitUntil(timeoutMillis = 5_000) { host.snackbar.currentSnackbarData != null }
    }
}
