package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.data.download.startDownloads
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.saver.plainFileUris
import eu.kanade.tachiyomi.source.Source
import exh.source.EH_SOURCE_ID
import exh.source.MERGED_SOURCE_ID
import exh.source.mangaDexSourceIds
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

/** One chapter-list pass: which entries are fetched, and what their new chapters trigger. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateChaptersTest : LibraryUpdateTestBase() {

    @After
    fun restoreMangaDexIds() {
        mangaDexSourceIds = emptyList()
    }

    private fun fetches(manga: Manga, chapters: List<Chapter>, stillFavorite: Boolean = true) {
        coEvery { getManga.await(manga.id) } returns manga
        coEvery { updateFromRemote(any<Source>(), manga, any(), any(), any(), any(), any()) } returns
            Result.success(RemoteMangaUpdate(manga.copy(favorite = stillFavorite), chapters))
    }

    private fun fails(manga: Manga, error: Throwable) {
        coEvery { getManga.await(manga.id) } returns manga
        coEvery { updateFromRemote(any<Source>(), manga, any(), any(), any(), any(), any()) } returns
            Result.failure(error)
    }

    private suspend fun runOver(vararg manga: Manga): LibraryUpdateJob = job().apply {
        mangaToUpdate = manga.map { libraryManga(it) }
        updateChapterList()
    }

    @Test
    fun newChaptersAreDownloaded() = runTest {
        val one = manga(1L)
        fetches(one, listOf(chapter(1L), chapter(2L)))
        coEvery { filterChapters.await(one, any()) } returns listOf(chapter(2L))
        runOver(one)
        verify { downloadManager.downloadChapters(one, listOf(chapter(2L)), false) }
        libraryPreferences.newUpdatesCount.get() shouldBe 2
        verify { downloadManager.startDownloads() }
        shown(Notifications.ID_NEW_CHAPTERS) shouldNotBe null
    }

    @Test
    fun nothingToDownloadOnlyNotifies() = runTest {
        val one = manga(1L)
        val two = manga(2L)
        fetches(one, listOf(chapter(1L)))
        fetches(two, emptyList())
        coEvery { filterChapters.await(one, any()) } returns emptyList()
        runOver(one, two)
        verify(exactly = 0) { downloadManager.downloadChapters(any(), any(), any()) }
        libraryPreferences.newUpdatesCount.get() shouldBe 1
        verify(exactly = 0) { downloadManager.startDownloads() }
    }

    @Test
    fun nonLibraryEntriesSkipped() = runTest {
        val gone = manga(1L)
        val unfavorite = manga(2L)
        coEvery { getManga.await(1L) } returns null
        coEvery { getManga.await(2L) } returns unfavorite.copy(favorite = false)
        fetches(manga(3L), listOf(chapter(1L)), stillFavorite = false)
        runOver(gone, unfavorite, manga(3L))
        coVerify(exactly = 1) { updateFromRemote(any<Source>(), any(), any(), any(), any(), any(), any()) }
        shown(Notifications.ID_NEW_CHAPTERS) shouldBe null
    }

    @Test
    fun excludedSourcesAreSkipped() = runTest {
        runOver(manga(1L, source = EH_SOURCE_ID))
        coVerify(exactly = 0) { getManga.await(any<Long>()) }
    }

    @Test
    fun failuresAreReported() = runTest {
        plainFileUris()
        fails(manga(1L), tachiyomi.domain.chapter.model.NoChaptersException())
        fails(manga(2L), tachiyomi.domain.source.model.SourceNotInstalledException())
        fails(manga(3L, source = 2L), IllegalStateException("broken"))
        runOver(manga(1L), manga(2L), manga(3L, source = 2L))
        shown(Notifications.ID_LIBRARY_ERROR) shouldNotBe null
        val report = context.externalCacheDir!!.resolve("mihon_update_errors.txt").readText()
        report.contains("! broken") shouldBe true
        report.contains("    - Manga 3") shouldBe true
    }

    @Test
    fun mangaDexEntriesGetTracks() = runTest {
        mangaDexSourceIds = listOf(7L)
        coEvery { mdList.isLoggedIn } returns true
        coEvery { getTracks.await(any<Long>()) } returns emptyList()
        fetches(manga(1L, source = 7L), emptyList())
        fetches(manga(2L), emptyList())
        runOver(manga(1L, source = 7L), manga(2L))
        coVerify(exactly = 1) { getTracks.await(1L) }
        coVerify(exactly = 0) { getTracks.await(2L) }
    }

    @Test
    fun loggedOutMangaDexHasNoTracks() = runTest {
        mangaDexSourceIds = listOf(7L)
        fetches(manga(1L, source = 7L), emptyList())
        runOver(manga(1L, source = 7L))
        coVerify(exactly = 0) { getTracks.await(any<Long>()) }
    }

    @Test
    fun mergedDownloadsGoToChildren() = runTest {
        val merged = manga(1L, source = MERGED_SOURCE_ID)
        val child = manga(11L, source = 3L)
        coEvery { getMerged.await(1L) } returns listOf(child)
        val chapters = listOf(chapter(1L, mangaId = 11L), chapter(2L, mangaId = 12L))
        job().downloadChapters(merged, chapters)
        verify { downloadManager.downloadChapters(child, listOf(chapters[0]), false) }
        verify(exactly = 1) { downloadManager.downloadChapters(any(), any(), any()) }
    }
}
