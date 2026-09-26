package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.libraryManga
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import exh.source.EH_SOURCE_ID
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class LibraryModelObserversTest {
    private val harness = LibraryHarness()
    private val reading = Category(id = 1, name = "Reading", order = 1, flags = 0)

    @Before
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
        coEvery { harness.getIdsWithMetadata.await() } returns emptyList()
        coEvery { harness.getTracks.await() } returns emptyList()
    }

    @After
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    @Test
    fun emptyLibraryShowsTheDefault() {
        val state = harness.model().state.await { !it.isLoading }
        state.displayedCategories.single().id shouldBe 0L
        state.libraryData.showSystemCategory shouldBe false
    }

    @Test
    fun favouritesAreGrouped() {
        harness.categories.value = listOf(reading)
        harness.library.value = listOf(libraryManga(1, manga(1), categories = listOf(1L)))
        val model = harness.model()
        model.state.await { it.groupedFavorites[reading] == listOf(1L) }
        harness.library.value = listOf(libraryManga(1, manga(1), categories = listOf(0L)))
        model.state.await { it.libraryData.showSystemCategory }
    }

    @Test
    fun searchNarrowsTheFavourites() {
        harness.library.value = listOf(libraryManga(1, manga(1, "Alpha")), libraryManga(2, manga(2, "Beta")))
        val model = harness.model()
        model.state.await { it.libraryData.favorites.size == 2 }
        model.search("alp")
        model.state.await { it.libraryData.favorites.map(LibraryItem::id) == listOf(1L) }.searchQuery shouldBe "alp"
    }

    @Test
    fun badgesFollowPreferences() {
        val merged = manga(2).copy(source = MERGED_SOURCE_ID)
        coEvery { harness.getMergedMangaById.await(2L) } returns listOf(manga(3), manga(4))
        every { harness.downloadManager.getDownloadCount(any<Manga>()) } returns 2
        every { harness.sourceManager.getOrStub(any()).lang } returns "en"
        harness.libraryPreferences.downloadBadge.set(true)
        harness.libraryPreferences.languageBadge.set(true)
        harness.libraryPreferences.localBadge.set(true)
        harness.library.value = listOf(libraryManga(1, manga(1).copy(source = 0)), libraryManga(2, merged))
        val favorites = harness.model().state.await { it.libraryData.favorites.size == 2 }.libraryData.favorites
        favorites.map { it.downloadCount } shouldBe listOf(2, 4)
        favorites.map { it.badges.sourceLanguage } shouldBe listOf("en", "en")
        favorites.map { it.badges.isLocal } shouldBe listOf(true, false)
    }

    @Test
    fun hiddenBadgesAreBlank() {
        harness.libraryPreferences.downloadBadge.set(false)
        harness.libraryPreferences.unreadBadge.set(false)
        harness.libraryPreferences.localBadge.set(false)
        harness.library.value = listOf(libraryManga(1, manga(1).copy(source = 0), totalChapters = 3))
        val state = harness.model().state.await { it.libraryData.favorites.size == 1 }
        val badges = state.libraryData.favorites.single().badges
        badges shouldBe LibraryItem.Badges(downloadCount = 0, unreadCount = 0, isLocal = false, sourceLanguage = "")
    }

    @Test
    fun trackerFiltersAreActive() {
        val tracker = mockk<BaseTracker> { every { id } returns 7L }
        harness.loggedIn.value = listOf(tracker)
        val model = harness.model()
        model.state.await { it.libraryData.loggedInTrackerIds == setOf(7L) }.hasActiveFilters shouldBe false
        harness.libraryPreferences.filterTracking(7).set(TriState.ENABLED_IS)
        model.state.await { it.hasActiveFilters }
    }

    @Test
    fun displayPreferencesReach() {
        val model = harness.model()
        harness.libraryPreferences.categoryTabs.set(true)
        harness.libraryPreferences.categoryNumberOfItems.set(true)
        harness.libraryPreferences.showContinueReadingButton.set(true)
        model.state.await { it.showCategoryTabs && it.showMangaCount && it.showMangaContinueButton }
    }

    @Test
    fun groupingUsesTheSortingMode() {
        harness.library.value = listOf(libraryManga(1, manga(1)))
        harness.libraryPreferences.groupLibraryBy.set(LibraryGroup.BY_STATUS)
        val state = harness.model().state.await { !it.isLoading && it.groupType == LibraryGroup.BY_STATUS }
        state.displayedCategories.size shouldBe 1
    }

    @Test
    fun exhSyncAndSyncService() {
        harness.exhPreferences.isHentaiEnabled.set(false)
        val model = harness.model()
        model.state.await { !it.showSyncExh }
        harness.exhPreferences.isHentaiEnabled.set(true)
        harness.sourcePreferences.disabledSources.set(setOf(EH_SOURCE_ID.toString()))
        harness.exhPreferences.enableExhentai.set(false)
        model.state.await { !it.showSyncExh }
        harness.exhPreferences.enableExhentai.set(true)
        model.state.await { it.showSyncExh }
        harness.syncPreferences.syncService.set(1)
        model.state.await { it.isSyncEnabled }
    }
}
