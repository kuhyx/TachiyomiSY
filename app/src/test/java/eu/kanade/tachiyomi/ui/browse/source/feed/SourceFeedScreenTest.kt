package eu.kanade.tachiyomi.ui.browse.source.feed

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.labelShown
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import eu.kanade.tachiyomi.ui.browse.feed.feed
import eu.kanade.tachiyomi.ui.browse.feed.savedSearch
import exh.source.mangaDexSourceIds
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The per-source feed screen: its rows, the filter sheet, search, and the feed dialogs. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class SourceFeedScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = SourceFeedHarness()
    private val loaded = MutableStateFlow(true)
    private val previousMangaDex = mangaDexSourceIds

    @Before
    fun setUp() {
        resetUiDispatcher()
        every { harness.sourceManager.isInitialized } returns loaded
        every { harness.getManga.subscribe(any<String>(), any()) } returns flowOf(null)
        coEvery { harness.source.getPopularManga(1) } returns page("Pop manga")
        coEvery { harness.source.getLatestUpdates(1) } returns page("New manga")
        harness.start()
    }

    @After
    fun tearDown() {
        mangaDexSourceIds = previousMangaDex
        harness.stop()
    }

    private fun show() {
        compose.setContent { ScreenHost(SourceFeedScreen(1L)) }
        compose.pollLabel("Pop manga")
    }

    private fun page(title: String) = MangasPage(
        listOf(
            SManga.create().apply {
                url = "/$title"
                this.title = title
            },
        ),
        false,
    )

    private fun click(label: String) = compose.clickLabel(label)

    @Test
    fun waitsForSources() {
        loaded.value = false
        compose.setContent { ScreenHost(SourceFeedScreen(1L)) }
        compose.waitForIdle()
        loaded.value = true
        compose.pollLabel("Pop manga")
    }

    @Test
    fun rowsOpenTheirListings() {
        show()
        click("Latest")
        compose.pollLabel("opened:BrowseSourceScreen")
    }

    @Test
    fun browseRowOpensPopular() {
        show()
        click("Browse")
        compose.pollLabel("opened:BrowseSourceScreen")
    }

    @Test
    fun mangaOpensDetails() {
        show()
        click("Pop manga")
        compose.pollLabel("opened:MangaScreen")
    }

    @Test
    fun savedFeedRowsOpenOrDelete() {
        coEvery { harness.getSearches.await(1L) } returns listOf(savedSearch(5L, "[]"))
        harness.feeds.value = listOf(feed(1L, savedSearch = 5L))
        show()
        compose.pollLabel("Search 5")
        compose.onAllNodes(hasText("Search 5")).onLast().performTouchInput { longClick() }
        compose.pollLabel("Delete")
        click("Delete")
        coVerify(timeout = 5_000) { harness.delete.await(1L) }
        click("Search 5")
        compose.pollLabel("opened:BrowseSourceScreen")
    }

    @Test
    fun searchBrowsesAndBackCloses() {
        show()
        click("Search")
        compose.onNode(hasSetTextAction()).performTextInput("needle")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        if (!compose.labelShown("needle")) {
            click("Search")
            compose.onNode(hasSetTextAction()).performTextInput("needle")
        }
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.pollLabel("opened:BrowseSourceScreen")
    }

    @Test
    fun filterSheetBrowses() {
        show()
        click("Filter")
        compose.pollLabel("Reset")
        click("Reset")
        click("Filter")
        compose.pollLabel("opened:BrowseSourceScreen")
    }

    @Test
    fun savedSearchChipsBrowseOrAdd() {
        show()
        click("Filter")
        compose.pollLabel("A")
        compose.onAllNodes(hasText("A"), useUnmergedTree = true).onLast().performTouchInput { longClick() }
        compose.pollLabel("Add")
        click("Add")
        coVerify(timeout = 5_000) { harness.insert.await(any()) }
        click("Filter")
        compose.pollLabel("b")
        click("b")
        compose.pollLabel("opened:BrowseSourceScreen")
    }

    @Test
    fun mangaDexButtons() {
        mangaDexSourceIds = listOf(1L)
        show()
        click("Filter")
        compose.pollLabel("Random")
        click("Random")
        click("MangaDex follows")
        compose.pollLabel("opened:MangaDexFollowsScreen")
    }
}
