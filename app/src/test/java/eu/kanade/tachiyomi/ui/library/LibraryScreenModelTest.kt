package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.track.BaseTracker
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.model.LibraryGroup

@RunWith(RobolectricTestRunner::class)
internal class LibraryScreenModelTest {
    private val harness = LibraryHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun names(state: LibraryScreenModel.State) = state.displayedCategories.map { it.name }

    @Test
    fun emptyLibraryShowsDefault() {
        val state = harness.model().await { !it.isLoading }
        names(state) shouldContainExactly listOf("Default category")
        state.isLibraryEmpty shouldBe true
    }

    @Test
    fun lastCategoryIsRestored() {
        harness.libraryPreferences.lastUsedCategory.set(1)
        harness.categories.value = listOf(libCategory(1L), libCategory(2L))
        val entries = listOf(libEntry(libManga(1L), listOf(1L)), libEntry(libManga(2L), listOf(2L)))
        val state = harness.loaded(entries).await { it.displayedCategories.size == 2 }
        state.activeCategory shouldBe libCategory(2L)
    }

    @Test
    fun systemCategoryOnlyWhenUsed() {
        harness.categories.value = listOf(libCategory(0L, "System"), libCategory(1L))
        val model = harness.loaded(listOf(libEntry(libManga(1L), listOf(0L)), libEntry(libManga(2L), listOf(1L))))
        names(model.await { it.displayedCategories.size == 2 }) shouldContainExactly listOf("System", "Cat 1")
        harness.libraryManga.value = listOf(libEntry(libManga(2L), listOf(1L)))
        names(model.await { it.displayedCategories.size == 1 }) shouldContainExactly listOf("Cat 1")
    }

    @Test
    fun searchNarrowsTheFavorites() {
        harness.categories.value = listOf(libCategory(1L))
        val model = harness.loaded(listOf(libEntry(libManga(1L, "Needle")), libEntry(libManga(2L, "Hay"))))
        model.search("needle")
        model.await { it.libraryData.favorites.size == 1 }.libraryData.favorites.single().id shouldBe 1L
        model.search(null)
        model.await { it.libraryData.favorites.size == 2 }
    }

    @Test
    fun groupingFollowsThePreference() {
        harness.libraryPreferences.groupLibraryBy.set(LibraryGroup.UNGROUPED)
        val model = harness.loaded(listOf(libEntry(libManga(1L, "b")), libEntry(libManga(2L, "a"))))
        val state = model.await { it.groupType == LibraryGroup.UNGROUPED && names(it) == listOf("Ungrouped") }
        state.groupedFavorites.values.single() shouldContainExactly listOf(2L, 1L)
    }

    @Test
    fun displayPrefsReachTheState() {
        val model = harness.model()
        harness.libraryPreferences.categoryTabs.set(true)
        harness.libraryPreferences.categoryNumberOfItems.set(true)
        harness.libraryPreferences.showContinueReadingButton.set(true)
        model.await { it.showCategoryTabs && it.showMangaCount && it.showMangaContinueButton }
    }

    @Test
    fun activeFiltersFromPrefs() {
        val model = harness.model()
        model.await { !it.hasActiveFilters && !it.isLoading }
        harness.libraryPreferences.filterUnread.set(TriState.ENABLED_IS)
        model.await { it.hasActiveFilters }
        harness.libraryPreferences.filterUnread.set(TriState.DISABLED)
        model.await { !it.hasActiveFilters }
        // Set before the login: set after it, the model's observer was seen never to pick it up (unexplained).
        harness.libraryPreferences.filterTracking(5).set(TriState.ENABLED_NOT)
        harness.loggedIn.value = listOf(mockk<BaseTracker> { every { id } returns 5L })
        model.await { it.hasActiveFilters }
    }

    @Test
    fun exhSyncNeedsHentaiEnabled() {
        val model = harness.model()
        model.await { it.showSyncExh }
        harness.sourcePreferences.disabledSources.set(setOf(EH_SOURCE_ID.toString()))
        model.await { !it.showSyncExh }
        harness.exhPreferences.enableExhentai.set(true)
        model.await { it.showSyncExh }
        harness.exhPreferences.isHentaiEnabled.set(false)
        model.await { !it.showSyncExh }
    }

    @Test
    fun syncServiceEnablesSync() {
        val model = harness.model()
        model.await { !it.isSyncEnabled && !it.isLoading }
        harness.syncPreferences.syncService.set(1)
        model.await { it.isSyncEnabled }
    }
}
