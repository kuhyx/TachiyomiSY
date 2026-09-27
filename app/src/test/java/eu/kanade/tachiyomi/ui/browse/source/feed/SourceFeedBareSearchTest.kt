package eu.kanade.tachiyomi.ui.browse.source.feed

import eu.kanade.tachiyomi.ui.browse.feed.feed
import eu.kanade.tachiyomi.ui.browse.feed.savedSearch
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CopyOnWriteArrayList

/** A saved-search feed with neither query nor filters searches blank; a picked saved search keeps the typed query. */
@RunWith(RobolectricTestRunner::class)
internal class SourceFeedBareSearchTest {
    private val harness = SourceFeedHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun bareSearchUsesDefaults() {
        harness.latest = false
        coEvery { harness.getSearches.await(1L) } returns listOf(savedSearch(5L, filtersJson = null, query = null))
        harness.feeds.value = listOf(feed(1L, savedSearch = 5L))
        val model = SourceFeedScreenModel(1L)
        eventually { model.state.value.items.size == 2 && model.state.value.items.all { it.results != null } }
        model.state.value.items.last().results?.single()?.title shouldBe "Found"
        coVerify { harness.source.getSearchManga(1, "", any()) }
    }

    @Test
    fun savedSearchKeepsTypedQuery() {
        val model = SourceFeedScreenModel(1L)
        eventually { model.state.value.filters.isNotEmpty() }
        model.search("typed")
        // Appended from the model's IO scope while the test thread reads it.
        val browsed = CopyOnWriteArrayList<String?>()
        model.onSavedSearch(harness.search(3L, "A"), { query, _ -> browsed += query }, {})
        eventually { browsed == listOf("typed") }
    }
}
