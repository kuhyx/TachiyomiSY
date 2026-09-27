package eu.kanade.tachiyomi.ui.browse.source.browse

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.model.EXHSavedSearch
import tachiyomi.i18n.sy.SYMR
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A saved search without filters is refused while the source has filters, and the listing stays put.
 * Main runs unconfined, so the toast hop returns without suspending.
 */
@RunWith(RobolectricTestRunner::class)
internal class SavedSearchRejectTest {
    private val harness = BrowseSourceHarness()

    // Appended from the model's IO scope while the test thread reads it.
    private val toasts = CopyOnWriteArrayList<StringResource>()

    @Before
    fun setUp() {
        mainUnconfined()
        harness.filters = { genreFilters() }
        harness.start()
    }

    @After
    fun tearDown() {
        try {
            harness.stop()
        } finally {
            mainReset()
        }
    }

    @Test
    fun filterlessSearchIsRefused() {
        val model = harness.model()
        model.setFilters(genreFilters())
        model.onSavedSearch(EXHSavedSearch(1L, "A", "q", null)) { toasts += it }
        eventually { toasts == listOf(SYMR.strings.save_search_invalid) }
        model.state.value.listing shouldBe BrowseSourceScreenModel.Listing.Popular
    }
}
