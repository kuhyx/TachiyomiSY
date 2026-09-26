package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.libraryManga
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import exh.favorites.FavoritesSyncStatus
import exh.recs.batch.SearchStatus
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryDisplayMode

@RunWith(RobolectricTestRunner::class)
internal class LibraryModelActionsTest {
    private val harness = LibraryHarness()
    private val reading = Category(id = 1, name = "Reading", order = 1, flags = 0)
    private val chapter = Chapter.create().copy(id = 3, mangaId = 1, name = "c3")

    @Before
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
    }

    @After
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    private fun loadedModel(): LibraryScreenModel {
        harness.categories.value = listOf(reading)
        harness.library.value = listOf(libraryManga(1, manga(1), categories = listOf(1L)))
        return harness.model().also { model -> model.state.await { it.groupedFavorites[reading] == listOf(1L) } }
    }

    @Test
    fun nextUnreadChapter() = runBlocking {
        coEvery { harness.getMergedMangaById.await(any()) } returns emptyList()
        coEvery { harness.getChaptersByMangaId.await(1L, applyScanlatorFilter = true) } returns listOf(chapter)
        // Pins issue #34: the merged check compares the manga id, not its source.
        coEvery { harness.getMergedChaptersByMangaId.await(MERGED_SOURCE_ID, applyScanlatorFilter = true) } returns
            emptyList()
        val model = harness.model()
        model.getNextUnreadChapter(manga(1)) shouldBe chapter
        model.getNextUnreadChapter(manga(MERGED_SOURCE_ID)).shouldBeNull()
    }

    @Test
    fun displayPreferencesAsState() {
        val model = harness.model()
        harness.libraryPreferences.landscapeColumns.set(5)
        harness.libraryPreferences.portraitColumns.set(2)
        model.getColumnsForOrientation(isLandscape = true).value shouldBe 5
        model.getColumnsForOrientation(isLandscape = false).value shouldBe 2
        model.getDisplayMode().value shouldBe harness.libraryPreferences.displayMode.get()
        model.getDisplayMode().value = LibraryDisplayMode.List
        harness.libraryPreferences.displayMode.get() shouldBe LibraryDisplayMode.List
    }

    @Test
    fun randomItemAndActiveCategory() {
        val model = loadedModel()
        model.randomItemInCurrentCategory()?.id shouldBe 1L
        model.updateActiveCategoryIndex(4)
        harness.libraryPreferences.lastUsedCategory.get() shouldBe 0
        model.updateState { it.copy(groupedFavorites = emptyMap()) }
        model.randomItemInCurrentCategory().shouldBeNull()
    }

    @Test
    fun selectionThroughTheModel() {
        val model = loadedModel()
        val manga = harness.library.value.single()
        model.toggleSelection(reading, manga)
        model.state.value.selection shouldBe setOf(1L)
        model.clearSelection()
        model.toggleRangeSelection(reading, manga)
        model.state.value.selection shouldBe setOf(1L)
        model.invertSelection()
        model.state.value.selection shouldBe emptySet()
        model.selectAll()
        model.state.value.selection shouldBe setOf(1L)
    }

    @Test
    fun firstUnreadIsTheFirstNext() = runBlocking {
        coEvery { harness.getNextChapters.await(1L) } returns listOf(chapter)
        coEvery { harness.getNextChapters.await(2L) } returns emptyList()
        val model = harness.model()
        model.getFirstUnread(manga(1)) shouldBe chapter
        model.getFirstUnread(manga(2)).shouldBeNull()
    }

    @Test
    fun recommendationSearchJob() {
        coEvery { harness.getLibraryManga.await() } coAnswers { awaitCancellation() }
        val model = harness.model()
        model.runRecommendationSearch(emptyList())
        val job = model.recommendationSearchJob!!
        // A search already running hands back no job, so the first one is kept.
        model.runRecommendationSearch(emptyList())
        model.recommendationSearchJob shouldBe job
        model.cancelRecommendationSearch()
        runBlocking { job.join() }
        job.isCancelled shouldBe true
        model.recommendationSearch.status.value shouldBe SearchStatus.Initializing
    }

    @Test
    fun noSearchJobToCancel() {
        harness.model().cancelRecommendationSearch()
    }

    @Test
    fun syncAndItsWarning() {
        val model = harness.model()
        model.favoritesSync.status.value = FavoritesSyncStatus.Initializing
        // A sync in progress ignores the request.
        model.runSync()
        model.favoritesSync.status.value shouldBe FavoritesSyncStatus.Initializing
        model.onAcceptSyncWarning()
        harness.exhPreferences.exhShowSyncIntro.get().shouldBeFalse()
        coVerify(exactly = 0) { harness.getLibraryManga.await() }
    }
}
