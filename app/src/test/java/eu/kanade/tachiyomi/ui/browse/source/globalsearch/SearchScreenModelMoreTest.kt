package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Queries that never search, an empty extension filter, and a search superseded mid-flight. */
@RunWith(RobolectricTestRunner::class)
internal class SearchScreenModelMoreTest {
    private val harness = SearchHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun missingQueryDoesNotSearch() {
        harness.source(1L)
        val model = GlobalSearchScreenModel()
        model.updateSearchQuery(null)
        model.search()
        model.updateSearchQuery("   ")
        model.search()
        model.state.value.total shouldBe 0
    }

    @Test
    fun emptyFilterSearchesAll() {
        harness.source(1L)
        harness.source(2L)
        val model = GlobalSearchScreenModel("q", initialExtensionFilter = "")
        eventually { model.state.value.total == 2 && model.state.value.progress == 2 }
    }

    // The superseded search's result is dropped; only the new query's result lands.
    @Test
    fun newQueryDropsOldResult() {
        harness.koin.sourcePreferences.pinnedSources.set(setOf("1"))
        val source = harness.source(1L)
        val gate = CompletableDeferred<Unit>()
        harness.gates[1L] = gate
        val model = GlobalSearchScreenModel("q")
        coVerify(timeout = 5_000) { source.getSearchManga(1, "q", any()) }
        model.updateSearchQuery("other")
        model.search()
        coVerify(timeout = 5_000) { source.getSearchManga(1, "other", any()) }
        gate.complete(Unit)
        eventually { model.state.value.progress == 1 }
        model.state.value.items.getValue(source).shouldBeInstanceOf<SearchItemResult.Success>()
    }
}
