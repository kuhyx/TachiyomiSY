package eu.kanade.tachiyomi.ui.browse.source.feed

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
import tachiyomi.i18n.sy.SYMR
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A saved search without filters is refused while the source has filters, and nothing is browsed.
 * Main runs unconfined, so the toast hop returns without suspending.
 */
@RunWith(RobolectricTestRunner::class)
internal class SourceFeedRejectTest {
    private val harness = SourceFeedHarness()

    // Appended from the model's IO scope while the test thread reads them.
    private val toasts = CopyOnWriteArrayList<StringResource>()
    private val browsed = CopyOnWriteArrayList<Long>()

    @Before
    fun setUp() {
        mainUnconfined()
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
        val model = SourceFeedScreenModel(1L)
        eventually { model.state.value.filters.isNotEmpty() }
        model.onSavedSearch(harness.search(2L, "b", filters = null), { _, id -> browsed += id }, { toasts += it })
        eventually { toasts == listOf(SYMR.strings.save_search_invalid) }
        browsed shouldBe emptyList()
    }
}
