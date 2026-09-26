package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.customInfoModule
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import tachiyomi.domain.chapter.interactor.GetBookmarkedChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.manga.interactor.GetMergedMangaById

internal class LibraryDownloadsTest {
    private val queued = mockk<Download> { every { chapter } returns chapter(id = 2) }
    private val downloadManager = mockk<DownloadManager>(relaxed = true) {
        every { queueState } returns MutableStateFlow(listOf(queued))
        every { isChapterDownloaded(any(), any(), any(), any(), any()) } answers { firstArg<String>() == "c3" }
    }
    private val getNextChapters = mockk<GetNextChapters>()
    private val getBookmarked = mockk<GetBookmarkedChaptersByMangaId>()
    private val getMergedMangaById = mockk<GetMergedMangaById>()
    private val downloads = LibraryDownloads(downloadManager, getNextChapters, getBookmarked, getMergedMangaById)
    private val plain by lazy { manga(1) }
    private val merged by lazy { manga(9).copy(source = MERGED_SOURCE_ID) }
    private val parts by lazy { listOf(manga(10), manga(11)) }
    private val fourChapters = (1L..4L).map { chapter(it) }

    @BeforeEach
    fun setUp() {
        startKoin { modules(customInfoModule()) }
        coEvery { getMergedMangaById.await(9L) } returns parts
    }

    @AfterEach
    fun tearDown() = stopKoin()

    private fun chapter(id: Long, mangaId: Long = 1) =
        Chapter.create().copy(id = id, mangaId = mangaId, name = "c$id", url = "/c$id")

    @Test
    fun nextSkipQueuedAndDownloaded() = runBlocking {
        coEvery { getNextChapters.await(1L) } returns fourChapters
        downloads.downloadNextChapters(listOf(plain), amount = 1)
        verify { downloadManager.downloadChapters(plain, listOf(fourChapters[0])) }
        downloads.downloadNextChapters(listOf(plain), amount = null)
        verify { downloadManager.downloadChapters(plain, listOf(fourChapters[0], fourChapters[3])) }
    }

    @Test
    fun mergedEntriesQueuePerPart() = runBlocking {
        val chapters = listOf(chapter(5, mangaId = 10), chapter(6, mangaId = 11), chapter(7, mangaId = 12))
        coEvery { getNextChapters.await(9L) } returns chapters
        downloads.downloadNextChapters(listOf(merged), amount = 2)
        verify { downloadManager.downloadChapters(parts[0], listOf(chapters[0])) }
        verify { downloadManager.downloadChapters(parts[1], listOf(chapters[1])) }
        verify(exactly = 0) { downloadManager.downloadChapters(merged, any()) }
    }

    @Test
    fun bookmarkedChapters() = runBlocking {
        coEvery { getBookmarked.await(1L) } returns fourChapters
        // Chapter 7 belongs to no known part and is dropped.
        coEvery { getBookmarked.await(9L) } returns listOf(chapter(2, mangaId = 10), chapter(7, mangaId = 12))
        downloads.downloadBookmarkedChapters(listOf(plain, merged))
        verify { downloadManager.downloadChapters(plain, listOf(fourChapters[0], fourChapters[3])) }
        verify { downloadManager.downloadChapters(parts[0], emptyList()) }
    }

    @Test
    fun limitToTakesAtMostTheAmount() {
        fourChapters.limitTo(null) shouldBe fourChapters
        fourChapters.limitTo(2) shouldBe fourChapters.take(2)
    }
}
