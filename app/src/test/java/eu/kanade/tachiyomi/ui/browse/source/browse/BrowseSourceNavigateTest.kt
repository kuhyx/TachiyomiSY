package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coVerify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.interactor.GetRemoteManga

/** Up leaves a typed search rather than clearing it, and saving searches with no query or unwritable filters. */
@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceNavigateTest {
    @get:Rule
    val compose = createComposeRule()

    private val rig = BrowseScreenRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun upLeavesATypedSearch() {
        val host = ScreenHost(BrowseSourceScreen(1L, "typed"))
        host.show(compose)
        rig.await("Navigate up")
        rig.click("Navigate up")
        host.top.shouldBeInstanceOf<BlankScreen>()
    }

    // Each save reads the toolbar when it runs, so every query is saved before the next is typed.
    @Test
    fun droppedQueriesSaveNone() {
        val model = rig.harness.model()
        listOf(null, " ", GetRemoteManga.QUERY_POPULAR).forEachIndexed { index, typed ->
            model.setToolbarQuery(typed)
            model.saveSearch("Saved $index")
            coVerify(timeout = 5_000) {
                rig.harness.insertSavedSearch.await(match { it.name == "Saved $index" && it.query == null })
            }
        }
    }

    // A filter the serializer cannot write is dropped, and the search is saved without filters.
    @Test
    fun brokenFiltersSaveBare() {
        val model = rig.harness.model()
        model.setFilters(FilterList(object : Filter.Select<Any>("Pick", arrayOf(Unprintable())) {}))
        model.saveSearch("Bare")
        coVerify(timeout = 5_000) {
            rig.harness.insertSavedSearch.await(match { it.name == "Bare" && it.filtersJson == null })
        }
    }

    private class Unprintable {
        override fun toString(): String = error("cannot print")
    }
}
