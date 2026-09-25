package eu.kanade.tachiyomi.ui.manga

import eu.kanade.domain.manga.interactor.UpdateManga
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.model.MangaWithChapterCount

@RunWith(RobolectricTestRunner::class)
internal class MangaLibraryTest {
    private val harness = MangaHarness()
    private val parts get() = harness.parts
    private val user = category(5L)

    private fun category(categoryId: Long): Category = mockk {
        every { id } returns categoryId
        every { isSystemCategory } returns false
    }

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga() to listOf(chapter(1L))
        coEvery { parts.getDuplicateLibraryManga(any()) } returns emptyList()
        coEvery { harness.updateManga.awaitUpdateFavorite(any(), any()) } returns true
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun duplicatesStopAtADialog() {
        val duplicate = MangaWithChapterCount(manga(), 3L)
        coEvery { parts.getDuplicateLibraryManga(any()) } returns listOf(duplicate)
        val model = harness.loaded()
        model.toggleFavorite()
        val dialog = model.awaitSuccess { it.dialog != null }.dialog
        dialog.shouldBeInstanceOf<MangaScreenModel.Dialog.DuplicateManga>().duplicates shouldBe listOf(duplicate)
    }

    @Test
    fun noCategoriesFavouritesDirectly() {
        val model = harness.loaded()
        model.toggleFavorite(onRemoved = {}, checkDuplicate = false)
        coVerify(timeout = 5_000) { parts.addTracks.bindEnhancedTrackers(any(), harness.source) }
        coVerify { parts.setMangaCategories.await(1L, emptyList()) }
        coVerify(exactly = 0) { parts.getDuplicateLibraryManga(any()) }
    }

    @Test
    fun defaultCategoryIsUsed() {
        coEvery { parts.getCategories.await() } returns listOf(category(0L), user)
        harness.libraryPreferences.defaultCategory.set(5)
        harness.loaded().toggleFavorite()
        coVerify(timeout = 5_000) { parts.setMangaCategories.await(1L, listOf(5L)) }
    }

    @Test
    fun failedFavouriteStops() {
        coEvery { harness.updateManga.awaitUpdateFavorite(any(), any()) } returns false
        harness.loaded().toggleFavorite()
        coVerify(timeout = 5_000) { harness.updateManga.awaitUpdateFavorite(1L, true) }
        coVerify(exactly = 0) { parts.addTracks.bindEnhancedTrackers(any(), any()) }
    }

    @Test
    fun unsetDefaultAsksForCategories() {
        coEvery { parts.getCategories.await() } returns listOf(user)
        coEvery { parts.getCategories.await(1L) } returns listOf(user)
        harness.libraryPreferences.defaultCategory.set(9)
        val model = harness.loaded()
        model.toggleFavorite()
        val dialog = model.awaitSuccess { it.dialog != null }.dialog
        val change = dialog.shouldBeInstanceOf<MangaScreenModel.Dialog.ChangeCategory>()
        change.initialSelection.single().value shouldBe user
        coVerify(timeout = 5_000) { parts.addTracks.bindEnhancedTrackers(any(), any()) }
    }

    @Test
    fun removingOffersDownloadDeletion() {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        every { harness.downloadManager.getDownloadCount(any()) } returns 2
        every { parts.coverCache.deleteFromCache(any(), any()) } returns 1
        val model = harness.loaded()
        model.toggleFavorite()
        eventually { model.snackbarHostState.currentSnackbarData != null }
        model.snackbarHostState.currentSnackbarData?.performAction()
        coVerify(timeout = 5_000) { harness.updateManga.awaitUpdateCoverLastModified(1L) }
    }

    @Test
    fun removingWithoutDownloads() {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        every { harness.downloadManager.getDownloadCount(any()) } returns 0
        every { parts.coverCache.deleteFromCache(any(), any()) } returns 0
        val model = harness.loaded()
        model.toggleFavorite()
        coVerify(timeout = 5_000) { harness.updateManga.awaitUpdateFavorite(1L, false) }
        coVerify(exactly = 0) { harness.updateManga.awaitUpdateCoverLastModified(any()) }
        model.snackbarHostState.currentSnackbarData shouldBe null
        coEvery { harness.updateManga.awaitUpdateFavorite(1L, false) } returns false
        var removed = false
        model.toggleFavorite(onRemoved = { removed = true })
        coVerify(timeout = 5_000, exactly = 2) { harness.updateManga.awaitUpdateFavorite(1L, false) }
        removed shouldBe false
    }

    @Test
    fun fetchIntervalIsSaved() {
        val field = UpdateManga::class.java.getDeclaredField("fetchInterval").apply { isAccessible = true }
        field.set(harness.updateManga, mockk<FetchInterval>(relaxed = true))
        val model = harness.loaded()
        model.library.showSetFetchIntervalDialog()
        model.awaitSuccess().dialog.shouldBeInstanceOf<MangaScreenModel.Dialog.SetFetchInterval>()
        coEvery { harness.updateManga.awaitUpdateFetchInterval(any()) } returnsMany listOf(false, true)
        coEvery { parts.mangaRepository.getMangaById(1L) } returns manga().copy(fetchInterval = -3)
        model.library.setFetchInterval(manga(), 3)
        model.library.setFetchInterval(manga(), 3)
        model.awaitSuccess { it.manga.fetchInterval == -3 }
        coVerify(exactly = 1) { parts.mangaRepository.getMangaById(1L) }
    }

    @Test
    fun addsIntoChosenCategories() {
        val model = harness.loaded()
        model.library.addToLibraryInCategories(manga(), listOf(5L))
        coVerify(timeout = 5_000) { harness.updateManga.awaitUpdateFavorite(1L, true) }
        model.library.addToLibraryInCategories(manga(favorite = true), listOf(6L))
        coVerify(timeout = 5_000) { parts.setMangaCategories.await(1L, listOf(6L)) }
        coVerify(exactly = 1) { harness.updateManga.awaitUpdateFavorite(1L, true) }
    }

    @Test
    fun loadingIgnoresLibraryCalls() {
        val model = harness.loading()
        model.toggleFavorite()
        model.library.showChangeCategoryDialog()
        model.library.showSetFetchIntervalDialog()
        model.state.value shouldBe MangaScreenModel.State.Loading
    }
}
