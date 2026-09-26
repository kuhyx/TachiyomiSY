package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.download.interactor.DeleteDownload
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

internal class SetReadStatusTest {

    private val downloadPreferences = DownloadPreferences(FlowPreferenceStore())
    private val deleteDownload = mockk<DeleteDownload>()
    private val mangaRepository = mockk<MangaRepository>()
    private val chapterRepository = mockk<ChapterRepository>()
    private val getMergedChaptersByMangaId = mockk<GetMergedChaptersByMangaId>()
    private val interactor = SetReadStatus(
        downloadPreferences = downloadPreferences,
        deleteDownload = deleteDownload,
        mangaRepository = mangaRepository,
        chapterRepository = chapterRepository,
        getMergedChaptersByMangaId = getMergedChaptersByMangaId,
    )
    private val unread = Chapter.create().copy(id = 1, mangaId = 4)
    private val read = Chapter.create().copy(id = 2, mangaId = 4, read = true)
    private val partlyRead = Chapter.create().copy(id = 3, mangaId = 5, lastPageRead = 7)

    @Test
    fun nothingToChange() = runTest {
        interactor.await(true, read) shouldBe SetReadStatus.Result.NoChapters
        interactor.await(false, unread) shouldBe SetReadStatus.Result.NoChapters
        coVerify(exactly = 0) { chapterRepository.updateAll(any()) }
    }

    @Test
    fun marksReadAndUnread() = runTest {
        val updates = slot<List<ChapterUpdate>>()
        coEvery { chapterRepository.updateAll(capture(updates)) } returns Unit
        interactor.await(true, unread, read) shouldBe SetReadStatus.Result.Success
        updates.captured shouldBe listOf(ChapterUpdate(id = 1, read = true))
        interactor.await(false, unread, read, partlyRead) shouldBe SetReadStatus.Result.Success
        updates.captured shouldBe listOf(
            ChapterUpdate(id = 2, read = false, lastPageRead = 0),
            ChapterUpdate(id = 3, read = false, lastPageRead = 0),
        )
    }

    @Test
    fun reportsRepositoryFailures() = runTest {
        val failure = IllegalStateException("db")
        coEvery { chapterRepository.updateAll(any()) } throws failure
        interactor.await(true, unread) shouldBe SetReadStatus.Result.InternalError(failure)
    }

    @Test
    fun deletesDownloadsWhenConfigured() = runTest {
        downloadPreferences.removeAfterMarkedAsRead.set(true)
        coEvery { chapterRepository.updateAll(any()) } returns Unit
        val manga4 = Manga.create().copy(id = 4)
        val manga5 = Manga.create().copy(id = 5)
        coEvery { mangaRepository.getMangaById(4) } returns manga4
        coEvery { mangaRepository.getMangaById(5) } returns manga5
        coEvery { deleteDownload.awaitAll(any(), *anyVararg()) } returns Unit
        interactor.await(true, unread, partlyRead) shouldBe SetReadStatus.Result.Success
        coVerify(exactly = 1) { deleteDownload.awaitAll(manga4, unread) }
        coVerify(exactly = 1) { deleteDownload.awaitAll(manga5, partlyRead) }
        interactor.await(false, read) shouldBe SetReadStatus.Result.Success
        coVerify(exactly = 2) { deleteDownload.awaitAll(any(), *anyVararg()) }
    }

    @Test
    fun loadsChaptersByManga() = runTest {
        coEvery { chapterRepository.getChapterByMangaId(4) } returns listOf(unread, read)
        coEvery { chapterRepository.updateAll(any()) } returns Unit
        interactor.await(4, true) shouldBe SetReadStatus.Result.Success
        interactor.await(Manga.create().copy(id = 4), true) shouldBe SetReadStatus.Result.Success
        coVerify(exactly = 2) { chapterRepository.updateAll(listOf(ChapterUpdate(id = 1, read = true))) }
    }

    @Test
    fun mergedMangaUseMergedChapters() = runTest {
        coEvery { getMergedChaptersByMangaId.await(4, dedupe = false) } returns listOf(unread)
        coEvery { chapterRepository.updateAll(any()) } returns Unit
        val merged = Manga.create().copy(id = 4, source = MERGED_SOURCE_ID)
        interactor.await(merged, true) shouldBe SetReadStatus.Result.Success
        coVerify(exactly = 1) { chapterRepository.updateAll(listOf(ChapterUpdate(id = 1, read = true))) }
        SetReadStatus.Result.Success.toString() shouldBe "Success"
        SetReadStatus.Result.NoChapters.toString() shouldBe "NoChapters"
    }

    // The non-cancellable blocks carry `read` across the lookup's suspension point; both values.
    @Test
    fun byIdMarksUnreadToo() = runTest {
        coEvery { chapterRepository.getChapterByMangaId(4) } returns listOf(read)
        coEvery { getMergedChaptersByMangaId.await(4, dedupe = false) } returns listOf(read)
        coEvery { chapterRepository.updateAll(any()) } returns Unit
        interactor.await(4, false) shouldBe SetReadStatus.Result.Success
        val merged = Manga.create().copy(id = 4, source = MERGED_SOURCE_ID)
        interactor.await(merged, false) shouldBe SetReadStatus.Result.Success
        coVerify(exactly = 2) { chapterRepository.updateAll(any()) }
    }
}
