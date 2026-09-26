package eu.kanade.tachiyomi.ui.library

import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.LibraryDisplayMode

@RunWith(RobolectricTestRunner::class)
internal class LibraryQueriesTest {
    private val harness = LibraryHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun nextUnreadReadsRightChapters() {
        val model = harness.model()
        runBlocking { model.getNextUnreadChapter(libManga(1L)) }.shouldBeNull()
        coVerify { harness.getChapters.await(1L, applyScanlatorFilter = true) }
        // The merged arm is keyed on the entry id, exactly as the main code compares it.
        runBlocking { model.getNextUnreadChapter(libManga(MERGED_SOURCE_ID)) }.shouldBeNull()
        coVerify { harness.getMergedChapters.await(MERGED_SOURCE_ID, any(), applyScanlatorFilter = true) }
    }

    @Test
    fun displayPreferencesAsState() {
        val model = harness.model()
        harness.libraryPreferences.landscapeColumns.set(5)
        harness.libraryPreferences.portraitColumns.set(3)
        model.getColumnsForOrientation(isLandscape = true).value shouldBe 5
        model.getColumnsForOrientation(isLandscape = false).value shouldBe 3
        harness.libraryPreferences.displayMode.set(LibraryDisplayMode.List)
        model.getDisplayMode().value shouldBe LibraryDisplayMode.List
    }

    @Test
    fun randomItemFromActiveCategory() {
        harness.categories.value = listOf(libCategory(1L))
        val model = harness.loaded(listOf(libEntry(libManga(1L))))
        model.await { it.activeCategory?.id == 1L }
        model.randomItemInCurrentCategory()?.id shouldBe 1L
    }

    @Test
    fun emptyLibraryHasNoRandomItem() {
        val model = harness.model()
        model.await { !it.isLoading }
        model.randomItemInCurrentCategory().shouldBeNull()
        // Before the first load there is no category at all.
        every { harness.getLibraryManga.subscribe() } returns flow { awaitCancellation() }
        harness.model().randomItemInCurrentCategory().shouldBeNull()
    }

    @Test
    fun invertDropsTheSelection() {
        harness.categories.value = listOf(libCategory(1L))
        val model = harness.selecting(libEntry(libManga(1L)))
        model.invertSelection()
        model.await { it.selection.isEmpty() }
    }

    @Test
    fun activeCategoryIsRemembered() {
        harness.categories.value = listOf(libCategory(1L), libCategory(2L))
        val model = harness.loaded(listOf(libEntry(libManga(1L), listOf(1L, 2L))))
        model.await { it.displayedCategories.size == 2 }
        model.updateActiveCategoryIndex(1)
        harness.libraryPreferences.lastUsedCategory.get() shouldBe 1
        model.updateActiveCategoryIndex(7)
        harness.libraryPreferences.lastUsedCategory.get() shouldBe 1
        model.state.value.activeCategory?.id shouldBe 2L
    }

    @Test
    fun explicitCollaboratorsAreKept() {
        val model = LibraryScreenModel(
            getLibraryManga = harness.getLibraryManga,
            getCategories = harness.getCategories,
            getTracksPerManga = harness.getTracksPerManga,
            getNextChapters = harness.getNextChapters,
            getChaptersByMangaId = harness.getChapters,
            setReadStatus = harness.setReadStatus,
            updateManga = harness.updateManga,
            setMangaCategories = harness.setMangaCategories,
            preferences = harness.basePreferences,
            libraryPreferences = harness.libraryPreferences,
            coverCache = harness.coverCache,
            sourceManager = harness.sourceManager,
            downloadManager = harness.downloadManager,
            downloadCache = harness.downloadCache,
            trackerManager = harness.trackerManager,
            exhPreferences = harness.exhPreferences,
            sourcePreferences = harness.sourcePreferences,
            getMergedMangaById = harness.getMergedManga,
            setCustomMangaInfo = harness.setCustomMangaInfo,
            getMergedChaptersByMangaId = harness.getMergedChapters,
            syncPreferences = harness.syncPreferences,
        )
        model.getLibraryManga shouldBe harness.getLibraryManga
        model.await { !it.isLoading }.isInitialized shouldBe false
    }
}
