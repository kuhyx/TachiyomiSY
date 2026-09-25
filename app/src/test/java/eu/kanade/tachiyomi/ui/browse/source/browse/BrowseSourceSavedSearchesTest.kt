package eu.kanade.tachiyomi.ui.browse.source.browse

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Dialog
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
import tachiyomi.domain.source.interactor.GetRemoteManga
import tachiyomi.domain.source.model.EXHSavedSearch
import tachiyomi.i18n.sy.SYMR

@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceSavedSearchesTest {
    private val harness = BrowseSourceHarness()
    private val toasts = mutableListOf<StringResource>()

    private class On(state: Boolean = false) : Filter.CheckBox("On", state) {
        override fun equals(other: Any?): Boolean = other is On && other.state == state

        override fun hashCode(): Int = state.hashCode()
    }

    @Before
    fun setUp() {
        harness.filters = { FilterList(On()) }
        harness.start()
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun saveDialogListsNames() {
        val model = harness.model()
        harness.savedSearches.value = listOf(EXHSavedSearch(1L, "A", null, null))
        eventually { model.state.value.savedSearches.size == 1 }
        model.onSaveSearch()
        eventually { model.state.value.dialog == Dialog.CreateSavedSearch(listOf("A")) }
        model.onSavedSearchPress(EXHSavedSearch(1L, "A", null, null))
        model.state.value.dialog shouldBe Dialog.DeleteSavedSearch(1L, "A")
    }

    @Test
    fun invalidSearchIsRejected() {
        val model = harness.model()
        model.onSavedSearch(EXHSavedSearch(1L, "A", "q", null)) { toasts += it }
        eventually { toasts == listOf(SYMR.strings.save_search_invalid) }
    }

    @Test
    fun savedSearchSetsTheListing() {
        val model = harness.model()
        model.onSavedSearch(EXHSavedSearch(1L, "A", "q", FilterList(On(true)))) { toasts += it }
        eventually { model.state.value.listing.query == "q" }
        model.state.value.filters shouldBe FilterList(On(true))
        model.onSavedSearch(EXHSavedSearch(2L, "B", "r", FilterList(On()))) { toasts += it }
        eventually { model.state.value.listing.query == "r" }
        model.state.value.filters shouldBe FilterList(On())
        model.setFilters(FilterList())
        model.onSavedSearch(EXHSavedSearch(3L, "C", "s", null)) { toasts += it }
        eventually { model.state.value.toolbarQuery == "s" }
        toasts shouldBe emptyList()
    }

    @Test
    fun searchesAreSaved() {
        val model = harness.model()
        model.setToolbarQuery(" q ")
        model.saveSearch(" Name ")
        coVerify(timeout = 5_000) {
            harness.insertSavedSearch.await(match { it.name == "Name" && it.query == "q" && it.filtersJson != null })
        }
        model.setToolbarQuery(GetRemoteManga.QUERY_LATEST)
        model.setFilters(FilterList())
        harness.filters = { FilterList() }
        model.saveSearch("Latest")
        coVerify(timeout = 5_000) {
            harness.insertSavedSearch.await(match { it.name == "Latest" && it.query == null && it.filtersJson == null })
        }
    }

    @Test
    fun blankQueriesAreDropped() {
        val model = harness.model()
        model.setToolbarQuery(" ")
        model.saveSearch("Blank")
        model.setToolbarQuery(GetRemoteManga.QUERY_POPULAR)
        model.saveSearch("Popular")
        model.setToolbarQuery(null)
        model.saveSearch("None")
        coVerify(timeout = 5_000, exactly = 3) { harness.insertSavedSearch.await(match { it.query == null }) }
        model.deleteSearch(4L)
        coVerify(timeout = 5_000) { harness.deleteSavedSearch.await(4L) }
    }

    @Test
    fun mangaDexRandomNeedsMangaDex() {
        val found = mutableListOf<String>()
        harness.model().onMangaDexRandom { found += it }
        val saved = mangaDexSourceIds
        mangaDexSourceIds = listOf(1L)
        val dex = mockk<MangaDex>(relaxed = true) {
            every { id } returns 1L
            every { getFilterList() } returns FilterList()
            coEvery { fetchRandomMangaUrl() } returns "/random"
        }
        every { harness.sourceManager.getOrStub(1L) } returns dex
        val model = harness.model()
        model.sourceIsMangaDex shouldBe true
        model.onMangaDexRandom { found += it }
        eventually { found == listOf("/random") }
        mangaDexSourceIds = saved
    }
}
