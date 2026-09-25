package eu.kanade.tachiyomi.ui.browse.source.feed

import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.browse.SourceFeedUI
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.browse.feed.feed
import eu.kanade.tachiyomi.ui.browse.feed.savedSearch
import eu.kanade.tachiyomi.ui.manga.eventually
import exh.source.mangaDexSourceIds
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.sy.SYMR

@RunWith(RobolectricTestRunner::class)
internal class SourceFeedScreenModelTest {
    private val harness = SourceFeedHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun model() = SourceFeedScreenModel(1L).also { model ->
        eventually { model.state.value.items.isNotEmpty() && model.state.value.items.all { it.results != null } }
    }

    @Test
    fun fixedFeedsLoad() {
        val model = model()
        model.state.value.items.map { it.results?.single()?.title } shouldBe listOf("Latest", "Popular")
        eventually { model.state.value.savedSearches.map { it.name } == listOf("A", "b") }
        model.sourceIsMangaDex shouldBe false
        model.startExpanded shouldBe harness.koin.uiPreferences.expandFilters.get()
        model.onDispose()
    }

    @Test
    fun savedSearchFeedsLoad() {
        harness.latest = false
        coEvery { harness.getSearches.await(1L) } returns listOf(savedSearch(5L, "[]"), savedSearch(6L, "bad"))
        harness.feeds.value = listOf(feed(1L, savedSearch = 5L), feed(2L, savedSearch = 6L))
        val items = model().state.value.items
        items.map { it::class } shouldBe
            listOf(SourceFeedUI.Browse::class, SourceFeedUI.SourceSavedSearch::class, SourceFeedUI.SourceSavedSearch::class)
        items[1].results?.single()?.title shouldBe "Found"
    }

    @Test
    fun failingFeedIsEmpty() {
        coEvery { harness.source.getPopularManga(1) } throws IllegalStateException("down")
        model().state.value.items.last().results shouldBe emptyList()
    }

    @Test
    fun feedsAreCreatedAndDeleted() {
        val model = model()
        model.createFeed(5L)
        model.deleteFeed(feed(4L))
        coVerify(timeout = 5_000) { harness.insert.await(feed(-1L, savedSearch = 5L).copy(global = false)) }
        coVerify(timeout = 5_000) { harness.delete.await(4L) }
    }

    @Test
    fun dialogsOpenAndClose() {
        val model = model()
        model.search("q")
        model.state.value.searchQuery shouldBe "q"
        model.openFilterSheet()
        model.state.value.dialog shouldBe SourceFeedScreenModel.Dialog.Filter
        model.openDeleteFeed(feed(1L))
        model.state.value.dialog shouldBe SourceFeedScreenModel.Dialog.DeleteFeed(feed(1L))
        model.openAddFeed(5L, "A")
        model.state.value.dialog shouldBe SourceFeedScreenModel.Dialog.AddFeed(5L, "A")
        model.dismissDialog()
        model.state.value.dialog shouldBe null
        SourceFeedState().isLoading shouldBe true
    }

    @Test
    fun filterBrowsesWithChanges() {
        val model = model()
        val calls = mutableListOf<Pair<String?, String?>>()
        model.search(" ")
        model.onFilter { query, filters -> calls += query to filters }
        eventually { calls.size == 1 }
        calls.single().first shouldBe null
        model.setFilters(FilterList(Flag(true)))
        model.search("q")
        model.onFilter { query, filters -> calls += query to filters }
        eventually { calls.size == 2 }
        calls.last().first shouldBe "q"
        (calls.last().second != null) shouldBe true
    }

    @Test
    fun savedSearchesBrowseOrWarn() {
        val model = model()
        val browsed = mutableListOf<Long>()
        val toasts = mutableListOf<StringResource>()
        model.onSavedSearch(harness.search(2L, "b", filters = null), { _, id -> browsed += id }, { toasts += it })
        eventually { toasts == listOf(SYMR.strings.save_search_invalid) }
        model.onSavedSearch(harness.search(3L, "A"), { _, id -> browsed += id }, { toasts += it })
        eventually { browsed == listOf(3L) }
        model.onSavedSearch(harness.search(4L, "D", FilterList(Flag())), { _, id -> browsed += id }, { toasts += it })
        model.setFilters(FilterList())
        model.onSavedSearch(harness.search(5L, "E", filters = null), { _, id -> browsed += id }, { toasts += it })
        eventually { browsed == listOf(3L, 5L) }
    }

    @Test
    fun addToFeedRespectsTheLimit() {
        val model = model()
        val toasts = mutableListOf<StringResource>()
        model.onSavedSearchAddToFeed(harness.search(3L, "A")) { toasts += it }
        eventually { model.state.value.dialog == SourceFeedScreenModel.Dialog.AddFeed(3L, "A") }
        coEvery { harness.count.await(1L) } returns 11L
        model.onSavedSearchAddToFeed(harness.search(3L, "A")) { toasts += it }
        eventually { toasts == listOf(SYMR.strings.too_many_in_feed) }
    }

    @Test
    fun mangaDexRandomNeedsMangaDex() {
        val found = mutableListOf<String>()
        model().onMangaDexRandom { found += it }
        val saved = mangaDexSourceIds
        mangaDexSourceIds = listOf(1L)
        val dex = mockk<MangaDex>(relaxed = true) {
            every { id } returns 1L
            every { getFilterList() } returns FilterList()
            coEvery { fetchRandomMangaUrl() } returns "/random"
        }
        every { harness.sourceManager.getOrStub(1L) } returns dex
        val model = SourceFeedScreenModel(1L)
        model.sourceIsMangaDex shouldBe true
        model.onMangaDexRandom { found += it }
        eventually { found == listOf("/random") }
        mangaDexSourceIds = saved
    }
}
