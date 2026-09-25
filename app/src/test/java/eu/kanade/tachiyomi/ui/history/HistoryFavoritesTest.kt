package eu.kanade.tachiyomi.ui.history

import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount

internal class HistoryFavoritesTest {
    private val harness = HistoryHarness()
    private val manga = Manga.create().copy(id = 7L, source = 3L)

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
        coEvery { harness.getCategories.await() } returns listOf(category(0), category(1), category(2))
        coEvery { harness.getCategories.await(7L) } returns listOf(category(2))
        coEvery { harness.getManga.await(7L) } returns manga
        coEvery { harness.getDuplicate(manga) } returns emptyList()
        coEvery { harness.updateManga.awaitUpdateFavorite(7L, true) } returns true
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    @Test
    fun movingSetsTheCategories() {
        val model = harness.model()
        model.moveMangaToCategory(7L, category(1))
        coVerify(timeout = WAIT) { harness.setMangaCategories.await(7L, listOf(1L)) }
        model.moveMangaToCategory(7L, categories = null)
        coVerify(timeout = WAIT) { harness.setMangaCategories.await(7L, emptyList()) }
    }

    @Test
    fun addingToLibraryFavourites() {
        val model = harness.model()
        model.addToLibraryInCategories(manga.copy(favorite = true), listOf(1L))
        model.addToLibraryInCategories(manga, listOf(2L))
        coVerify(timeout = WAIT, exactly = 1) { harness.updateManga.awaitUpdateFavorite(7L, true) }
        coVerify(timeout = WAIT, exactly = 2) { harness.setMangaCategories.await(7L, any()) }
    }

    @Test
    fun unknownMangaIsIgnored() {
        coEvery { harness.getManga.await(8L) } returns null
        harness.model().addFavorite(8L)
        coVerify(timeout = WAIT) { harness.getManga.await(8L) }
        coVerify(exactly = 0) { harness.getDuplicate(any()) }
    }

    @Test
    fun duplicatesOpenADialog() {
        val duplicates = listOf(MangaWithChapterCount(manga.copy(id = 9L), chapterCount = 3))
        coEvery { harness.getDuplicate(manga) } returns duplicates
        val model = harness.model()
        model.addFavorite(7L)
        val dialog = model.state.await { it.dialog != null }.dialog
        dialog shouldBe HistoryScreenModel.Dialog.DuplicateManga(manga, duplicates)
    }

    @Test
    fun defaultCategoryIsUsed() {
        harness.libraryPreferences.defaultCategory.set(1)
        harness.model().addFavorite(7L)
        coVerify(timeout = WAIT) { harness.setMangaCategories.await(7L, listOf(1L)) }
        coVerify(timeout = WAIT) { harness.addTracks.bindEnhancedTrackers(manga, harness.source) }
    }

    @Test
    fun automaticDefaultClears() {
        harness.libraryPreferences.defaultCategory.set(0)
        harness.model().addFavorite(manga)
        coVerify(timeout = WAIT) { harness.setMangaCategories.await(7L, emptyList()) }
        coVerify(timeout = WAIT) { harness.addTracks.bindEnhancedTrackers(manga, harness.source) }
    }

    @Test
    fun noCategoriesClearsCategories() {
        coEvery { harness.getCategories.await() } returns listOf(category(0))
        harness.model().addFavorite(manga)
        coVerify(timeout = WAIT) { harness.setMangaCategories.await(7L, emptyList()) }
    }

    @Test
    fun failedFavouriteStops() {
        coEvery { harness.updateManga.awaitUpdateFavorite(7L, true) } returns false
        harness.libraryPreferences.defaultCategory.set(1)
        harness.model().addFavorite(manga)
        coVerify(timeout = WAIT, exactly = 1) { harness.updateManga.awaitUpdateFavorite(7L, true) }
        harness.libraryPreferences.defaultCategory.set(0)
        harness.model().addFavorite(manga)
        coVerify(timeout = WAIT, exactly = 2) { harness.updateManga.awaitUpdateFavorite(7L, true) }
        coVerify(exactly = 0) { harness.setMangaCategories.await(any(), any()) }
    }

    @Test
    fun otherwiseCategoriesAreAsked() {
        val model = harness.model()
        model.addFavorite(manga)
        val dialog = model.state.await { it.dialog != null }.dialog
        dialog shouldBe HistoryScreenModel.Dialog.ChangeCategory(
            manga = manga,
            initialSelection = listOf(CheckboxState.State.None(category(1)), CheckboxState.State.Checked(category(2))),
        )
    }
}

private const val WAIT = 5_000L
