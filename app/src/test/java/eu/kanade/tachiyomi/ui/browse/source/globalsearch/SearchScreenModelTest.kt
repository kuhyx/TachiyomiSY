package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import eu.kanade.domain.extension.interactor.installed
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateSearchScreenModel
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.manga.manga
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SearchScreenModelTest {
    private val harness = SearchHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun SearchScreenModel.settled(count: Int) =
        eventually { state.value.total == count && state.value.progress == count }

    @Test
    fun blankQueryDoesNotSearch() {
        harness.source(1L)
        val model = GlobalSearchScreenModel()
        model.search()
        model.state.value.total shouldBe 0
    }

    @Test
    fun pinnedOnlySearchesPinned() {
        harness.koin.sourcePreferences.pinnedSources.set(setOf("1"))
        harness.source(1L)
        harness.source(2L)
        val model = GlobalSearchScreenModel("q")
        model.settled(1)
        val result = model.state.value.items.values.single().shouldBeInstanceOf<SearchItemResult.Success>()
        result.result.single().title shouldBe "T1"
        result.isEmpty shouldBe false
    }

    @Test
    fun allSourcesReuseResults() {
        harness.koin.sourcePreferences.pinnedSources.set(setOf("1"))
        harness.source(1L)
        harness.source(2L, titles = null)
        harness.source(3L, sourceLang = "fr")
        harness.koin.sourcePreferences.disabledSources.set(setOf("4"))
        harness.source(4L)
        val model = GlobalSearchScreenModel("q")
        model.settled(1)
        model.setSourceFilter(SourceFilter.All)
        model.settled(2)
        model.state.value.items.values.last().shouldBeInstanceOf<SearchItemResult.Error>()
        coVerify(exactly = 1) { harness.catalogue[0].getSearchManga(1, "q", any()) }
        model.setSourceFilter(SourceFilter.All)
        model.updateSearchQuery("other")
        model.search()
        model.settled(2)
    }

    @Test
    fun extensionFilterNarrowsSources() {
        val ext = installed("Ext").copy(sources = listOf(harness.source(5L)))
        harness.source(6L)
        harness.installed.value = listOf(ext, installed("Else"))
        val model = GlobalSearchScreenModel("q", initialExtensionFilter = ext.pkgName)
        model.settled(1)
        model.state.value.sourceFilter shouldBe SourceFilter.All
        GlobalSearchScreenModel("", initialExtensionFilter = "").state.value.total shouldBe 0
        GlobalSearchScreenModel("", initialExtensionFilter = ext.pkgName).state.value.total shouldBe 0
    }

    @Test
    fun resultsCanBeFiltered() {
        harness.source(1L, titles = emptyList())
        val model = GlobalSearchScreenModel("q")
        model.setSourceFilter(SourceFilter.All)
        model.settled(1)
        model.state.value.filteredItems.size shouldBe 1
        model.toggleFilterResults()
        eventually { model.state.value.onlyShowHasResults }
        model.state.value.filteredItems.size shouldBe 0
    }

    @Test
    fun migrateDialogNeedsTheEntry() {
        coEvery { harness.getManga.await(1L) } returnsMany listOf(null, manga())
        val model = GlobalSearchScreenModel()
        model.setMigrateDialog(1L, manga().copy(id = 2L))
        coVerify(timeout = 5_000) { harness.getManga.await(1L) }
        model.state.value.dialog shouldBe null
        model.setMigrateDialog(1L, manga().copy(id = 2L))
        eventually { model.state.value.dialog != null }
        model.state.value.dialog.shouldBeInstanceOf<SearchScreenModel.Dialog.Migrate>().current shouldBe manga()
        model.clearDialog()
        model.state.value.dialog shouldBe null
    }

    @Test
    fun migrationSearchesItsSources() {
        harness.koin.sourcePreferences.migrationSources.set(listOf(2L, 1L, 9L))
        harness.source(1L, titles = emptyList())
        harness.source(2L)
        coEvery { harness.getManga.await(5L) } returns manga().copy(id = 5L, ogTitle = "Needle")
        val model = MigrateSearchScreenModel(5L)
        model.settled(2)
        model.state.value.searchQuery shouldBe "Needle"
        model.state.value.from?.id shouldBe 5L
        model.state.value.items.keys.map { it.id } shouldBe listOf(2L, 1L)
    }

    @Test
    fun resultVisibility() {
        SearchItemResult.Loading.isVisible(onlyShowHasResults = false) shouldBe true
        SearchItemResult.Loading.isVisible(onlyShowHasResults = true) shouldBe false
        SearchItemResult.Success(listOf(manga())).isVisible(onlyShowHasResults = true) shouldBe true
        SearchItemResult.Error(IllegalStateException()).copy().isVisible(onlyShowHasResults = true) shouldBe false
        SearchScreenModel.Dialog.Migrate(manga(), manga()).copy().target shouldBe manga()
    }
}
