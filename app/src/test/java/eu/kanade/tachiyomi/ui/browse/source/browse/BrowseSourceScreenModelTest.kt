package eu.kanade.tachiyomi.ui.browse.source.browse

import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.interactor.GetRemoteManga
import tachiyomi.domain.source.model.EXHSavedSearch

@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceScreenModelTest {
    private val harness = BrowseSourceHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun listingsParseTheQuery() {
        Listing.valueOf(GetRemoteManga.QUERY_POPULAR) shouldBe Listing.Popular
        Listing.valueOf(GetRemoteManga.QUERY_LATEST) shouldBe Listing.Latest
        Listing.valueOf("q") shouldBe Listing.Search("q", FilterList())
        val model = harness.model()
        model.state.value.listing shouldBe Listing.Popular
        model.state.value.isUserQuery shouldBe false
        model.state.value.toolbarQuery shouldBe null
    }

    @Test
    fun searchListingKeepsTheQuery() {
        val model = harness.model(listing = "q")
        model.state.value.isUserQuery shouldBe true
        model.state.value.toolbarQuery shouldBe "q"
        harness.model(listing = "").state.value.isUserQuery shouldBe false
    }

    @Test
    fun remembersSourceUnlessIncognito() {
        harness.model()
        harness.koin.sourcePreferences.lastUsedSource.get() shouldBe 1L
        harness.koin.sourcePreferences.lastUsedSource.set(-1L)
        every { harness.incognito.await(1L) } returns true
        harness.model()
        harness.koin.sourcePreferences.lastUsedSource.get() shouldBe -1L
    }

    @Test
    fun savedSearchIsApplied() {
        val search = EXHSavedSearch(5L, "Saved", "q", FilterList(object : Filter.CheckBox("On", true) {}))
        coEvery { harness.exhSavedSearch.awaitOne(5L, any()) } returns search
        val model = harness.model(savedSearch = 5L)
        model.state.value.listing.query shouldBe "q"
        model.state.value.filters shouldBe search.filterList
        harness.model(savedSearch = 6L).state.value.listing shouldBe Listing.Popular
    }

    @Test
    fun jsonFiltersAreApplied() {
        harness.filters = { FilterList(object : Filter.CheckBox("On") {}) }
        val model = harness.model(filtersJson = """[{"_cbClass":"CheckBox","name":"On","state":true}]""")
        model.state.value.listing.shouldBeInstanceOf<Listing.Search>()
        harness.model(filtersJson = "not json").state.value.listing shouldBe Listing.Popular
    }

    @Test
    fun savedSearchesAreSorted() {
        val model = harness.model()
        harness.savedSearches.value = listOf(EXHSavedSearch(1L, "b", null, null), EXHSavedSearch(2L, "A", null, null))
        eventually { model.state.value.savedSearches.map { it.name } == listOf("A", "b") }
    }

    @Test
    fun columnsFollowOrientation() {
        val model = harness.model()
        model.getColumnsPreference(Configuration.ORIENTATION_PORTRAIT) shouldBe GridCells.Adaptive(128.dp)
        harness.koin.libraryPreferences.landscapeColumns.set(4)
        model.getColumnsPreference(Configuration.ORIENTATION_LANDSCAPE) shouldBe GridCells.Fixed(4)
    }

    @Test
    fun dialogsAndToolbar() {
        val model = harness.model()
        model.openFilterSheet()
        model.state.value.dialog shouldBe BrowseSourceScreenModel.Dialog.Filter
        model.setDialog(null)
        model.setToolbarQuery("t")
        model.state.value.toolbarQuery shouldBe "t"
        model.displayMode shouldBe harness.koin.sourcePreferences.sourceDisplayMode.get()
        model.ehentaiBrowseDisplayMode shouldBe harness.koin.exhPreferences.enhancedEHentaiView.get()
        model.startExpanded shouldBe harness.koin.uiPreferences.expandFilters.get()
        model.sourceIsMangaDex shouldBe false
    }
}
