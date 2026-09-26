package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.chapter.interactor.GetBookmarkedChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.manga.interactor.GetMergedMangaById

internal class LibraryDownloadsTest {
    private val queue = MutableStateFlow<List<Download>>(emptyList())
    private val downloadManager = mockk<DownloadManager>(relaxed = true) {
        every { queueState } returns queue
        every { isChapterDownloaded(any(), any(), any(), any(), any(), any()) } returns false
        every { isChapterDownloaded("C2", any(), any(), any(), any(), any()) } returns true
    }
    private val getNextChapters = mockk<GetNextChapters>()
    private val getBookmarked = mockk<GetBookmarkedChaptersByMangaId>()
    private val getMergedManga = mockk<GetMergedMangaById>()
    private val downloads = LibraryDownloads(
        downloadManager = downloadManager,
        getNextChapters = getNextChapters,
        getBookmarkedChaptersByMangaId = getBookmarked,
        getMergedMangaById = getMergedManga,
    )

    private val plain = libManga(1L)
    private val merged = libManga(10L, source = MERGED_SOURCE_ID)
    private val part = libManga(11L, source = 2L)

    private val chapters = listOf(chapter(1L), chapter(2L), chapter(3L), chapter(4L))

    private fun chapter(id: Long, mangaId: Long = 1L): Chapter =
        Chapter.create().copy(id = id, mangaId = mangaId, name = "C$id", url = "/c/$id")

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun queued(chapter: Chapter) {
        queue.value = listOf(Download(source = mockk<HttpSource>(), manga = plain, chapter = chapter))
    }

    @Test
    fun nextChaptersSkipQueued() = runTest {
        queued(chapter(1L))
        coEvery { getNextChapters.await(1L, any<Boolean>()) } returns chapters
        downloads.downloadNextChapters(listOf(plain), 1)
        verify { downloadManager.downloadChapters(plain, listOf(chapter(3L)), any()) }
        downloads.downloadNextChapters(listOf(plain), null)
        verify { downloadManager.downloadChapters(plain, listOf(chapter(3L), chapter(4L)), any()) }
    }

    @Test
    fun mergedChaptersGoToParts() = runTest {
        coEvery { getNextChapters.await(10L, any<Boolean>()) } returns
            listOf(chapter(5L, mangaId = 11L), chapter(6L, mangaId = 99L), chapter(7L, mangaId = 11L))
        coEvery { getMergedManga.await(10L) } returns listOf(part)
        downloads.downloadNextChapters(listOf(merged), 2)
        verify(exactly = 1) { downloadManager.downloadChapters(any(), any(), any()) }
        verify { downloadManager.downloadChapters(part, listOf(chapter(5L, mangaId = 11L)), any()) }
    }

    @Test
    fun bookmarkedChaptersPerEntry() = runTest {
        coEvery { getBookmarked.await(1L) } returns chapters
        coEvery { getBookmarked.await(10L) } returns listOf(chapter(8L, mangaId = 11L))
        coEvery { getMergedManga.await(10L) } returns listOf(part)
        downloads.downloadBookmarkedChapters(listOf(plain, merged))
        verify { downloadManager.downloadChapters(plain, listOf(chapter(1L), chapter(3L), chapter(4L)), any()) }
        verify { downloadManager.downloadChapters(part, listOf(chapter(8L, mangaId = 11L)), any()) }
    }

    @Test
    fun limitKeepsAllWithoutAmount() {
        chapters.limitTo(null) shouldContainExactly chapters
        chapters.limitTo(2) shouldContainExactly chapters.take(2)
    }

    @Test
    fun defaultsComeFromInjekt() {
        startKoin {
            modules(
                module {
                    single { downloadManager }
                    single { getNextChapters }
                    single { getBookmarked }
                    single { getMergedManga }
                },
            )
        }
        runTest { LibraryDownloads().downloadNextChapters(emptyList(), null) }
        verify(exactly = 0) { downloadManager.downloadChapters(any(), any(), any()) }
    }
}
