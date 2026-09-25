package eu.kanade.tachiyomi.ui.browse.source.browse

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceSearchTest {
    private val harness = BrowseSourceHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun BrowseSourceScreenModel.search(): Listing.Search = state.value.listing.shouldBeInstanceOf()

    @Test
    fun searchFromPopularStartsFresh() {
        val model = harness.model()
        model.search(query = "q")
        model.search().query shouldBe "q"
        model.state.value.toolbarQuery shouldBe "q"
    }

    @Test
    fun searchKeepsQueryAndFilters() {
        val model = harness.model(listing = "q")
        val filters = FilterList(object : Filter.CheckBox("On") {})
        model.search(filters = filters)
        model.search().query shouldBe "q"
        model.state.value.filters shouldBe filters
        model.search(filters = model.state.value.filters)
        model.search().filters shouldBe filters
        model.search()
        model.search().query shouldBe "q"
    }

    @Test
    fun filtersAndListingAreSet() {
        val model = harness.model(listing = "q")
        val filters = FilterList(object : Filter.CheckBox("On") {})
        model.setFilters(filters)
        model.state.value.filters shouldBe filters
        model.resetFilters()
        model.state.value.filters shouldBe FilterList()
        model.setListing(Listing.Latest)
        model.state.value.listing shouldBe Listing.Latest
        model.state.value.toolbarQuery shouldBe null
    }

    @Test
    fun genreSelectsTheTriState() {
        harness.filters = ::genreFilters
        val model = harness.model()
        model.searchGenre("action")
        model.search().query shouldBe null
        val group = model.search().filters.first() as Filter.Group<*>
        (group.state.first() as Filter.TriState).state shouldBe Filter.TriState.STATE_INCLUDE
    }

    @Test
    fun genreSelectsTheCheckbox() {
        harness.filters = ::genreFilters
        val model = harness.model()
        model.searchGenre("Comedy")
        val group = model.search().filters.first() as Filter.Group<*>
        (group.state[1] as Filter.CheckBox).state shouldBe true
    }

    @Test
    fun genreSelectsTheOption() {
        harness.filters = ::genreFilters
        val model = harness.model()
        model.searchGenre("oneshot")
        (model.search().filters[1] as Filter.Select<*>).state shouldBe 1
    }

    @Test
    fun unknownGenreBecomesAQuery() {
        harness.filters = ::genreFilters
        val model = harness.model()
        model.searchGenre("Odd")
        model.search().query shouldBe null
        model.searchGenre("Romance")
        model.search().query shouldBe "Romance"
        model.state.value.toolbarQuery shouldBe "Romance"
    }
}
