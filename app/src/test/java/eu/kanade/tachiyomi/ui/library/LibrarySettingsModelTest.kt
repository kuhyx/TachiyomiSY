package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort

@RunWith(RobolectricTestRunner::class)
internal class LibrarySettingsModelTest {
    private val harness = LibraryHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun defaultsComeFromInjekt() {
        val model = LibrarySettingsScreenModel()
        model.libraryPreferences shouldBe harness.libraryPreferences
        model.trackersFlow.value shouldBe emptyList()
        model.grouping shouldBe LibraryGroup.BY_DEFAULT
    }

    @Test
    fun filtersCycle() {
        val model = LibrarySettingsScreenModel()
        model.toggleFilter { it.filterUnread }
        harness.libraryPreferences.filterUnread.get() shouldBe TriState.ENABLED_IS
        model.toggleTracker(3)
        harness.libraryPreferences.filterTracking(3).get() shouldBe TriState.ENABLED_IS
    }

    @Test
    fun displaySortAndGroup() {
        val model = LibrarySettingsScreenModel()
        model.setDisplayMode(LibraryDisplayMode.List)
        harness.libraryPreferences.displayMode.get() shouldBe LibraryDisplayMode.List
        model.setSort(null, LibrarySort.Type.DateAdded, LibrarySort.Direction.Descending)
        coVerify(timeout = WAIT) {
            harness.setSortMode.await(null as Category?, LibrarySort.Type.DateAdded, LibrarySort.Direction.Descending)
        }
        model.setGrouping(LibraryGroup.BY_SOURCE)
        eventually { harness.libraryPreferences.groupLibraryBy.get() == LibraryGroup.BY_SOURCE }
    }
}
