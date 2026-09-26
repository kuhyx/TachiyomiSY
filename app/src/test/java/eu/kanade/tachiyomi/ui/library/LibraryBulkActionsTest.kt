package eu.kanade.tachiyomi.ui.library

import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.tachiyomi.data.download.deleteManga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.MangaUpdate

@RunWith(RobolectricTestRunner::class)
internal class LibraryBulkActionsTest {
    private val harness = LibraryHarness()

    @Before
    fun setUp() {
        harness.start()
        harness.categories.value = listOf(libCategory(1L))
    }

    @After
    fun tearDown() {
        harness.stop()
        unmockkAll()
    }

    /** A model over [entries] with all of them selected. */
    @Test
    fun downloadActionsQueueChapters() {
        val model = harness.selecting(libEntry(libManga(1L)))
        model.performDownloadAction(DownloadAction.BOOKMARKED_CHAPTERS)
        model.state.value.selection.shouldBeEmpty()
        coVerify(timeout = WAIT) { harness.getBookmarked.await(1L) }
        model.selectAll()
        model.await { it.selection.isNotEmpty() }
        model.performDownloadAction(DownloadAction.NEXT_5_CHAPTERS)
        coVerify(timeout = WAIT) { harness.getNextChapters.await(1L, any<Boolean>()) }
    }

    @Test
    fun markReadForEachSelected() {
        val model = harness.selecting(libEntry(libManga(1L)), libEntry(libManga(2L)))
        model.markReadSelection(true)
        model.state.value.selection.shouldBeEmpty()
        coVerify(timeout = WAIT) { harness.setReadStatus.await(manga = libManga(1L), read = true) }
        coVerify(timeout = WAIT) { harness.setReadStatus.await(manga = libManga(2L), read = true) }
    }

    @Test
    fun resetInfoClearsEveryEdit() {
        val model = harness.selecting(libEntry(libManga(1L)))
        model.resetInfo()
        verify { harness.setCustomMangaInfo.set(match { it.id == 1L && it.title == null && it.status == null }) }
        model.state.value.selection.shouldBeEmpty()
    }

    @Test
    fun removeFromLibraryOnly() {
        val model = harness.model()
        model.removeMangas(listOf(libManga(1L)), deleteFromLibrary = true, deleteChapters = false)
        coVerify(timeout = WAIT) { harness.updateManga.awaitAll(listOf(MangaUpdate(id = 1L, favorite = false))) }
        model.removeMangas(listOf(libManga(1L)), deleteFromLibrary = false, deleteChapters = false)
        coVerify(exactly = 1) { harness.updateManga.awaitAll(any()) }
    }

    @Test
    fun deleteChaptersPerSourceKind() {
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerDeletionKt")
        every { harness.downloadManager.deleteManga(any(), any(), any()) } just runs
        val http = mockk<HttpSource>(relaxed = true) { every { id } returns 2L }
        val merged = mockk<MergedSource>(relaxed = true)
        val plain = mockk<Source>(relaxed = true) { every { id } returns 3L }
        every { harness.sourceManager.get(any()) } returns null
        every { harness.sourceManager.get(2L) } returns http
        every { harness.sourceManager.get(3L) } returns plain
        every { harness.sourceManager.get(9L) } returns merged
        every { harness.sourceManager.getOrStub(2L) } returns http
        every { harness.sourceManager.getOrStub(3L) } returns plain
        val partOnHttp = libManga(21L, source = 2L)
        val partOnPlain = libManga(22L, source = 3L)
        coEvery { harness.getMergedManga.await(20L) } returns listOf(partOnHttp, partOnPlain)
        val mangas = listOf(
            libManga(1L, source = 2L),
            libManga(2L, source = 3L),
            libManga(3L, source = 4L),
            libManga(20L, source = 9L),
        )
        harness.model().removeMangas(mangas, deleteFromLibrary = false, deleteChapters = true)
        verify(timeout = WAIT) { harness.downloadManager.deleteManga(libManga(1L, source = 2L), http, any()) }
        verify(timeout = WAIT) { harness.downloadManager.deleteManga(partOnHttp, http, any()) }
        verify(exactly = 2) { harness.downloadManager.deleteManga(any(), any(), any()) }
    }

    @Test
    fun categoriesAreAddedAndRemoved() {
        coEvery { harness.getCategories.await(1L) } returns listOf(libCategory(1L), libCategory(2L))
        harness.model().setMangaCategories(
            mangaList = listOf(libManga(1L)),
            addCategories = listOf(3L),
            removeCategories = listOf(1L),
        )
        coVerify(timeout = WAIT) { harness.setMangaCategories.await(1L, listOf(2L, 3L)) }
    }
}
