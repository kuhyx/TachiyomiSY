package tachiyomi.domain.chapter.interactor

import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import java.io.IOException

internal class GetBookmarkedChaptersByMangaIdTest {

    private val bookmarked = Chapter.create().copy(id = 1L, mangaId = 7L, bookmark = true)
    private val plain = Chapter.create().copy(id = 2L, mangaId = 7L, bookmark = false)
    private val repository = mockk<ChapterRepository>()
    private val getManga = mockk<GetManga>()
    private val getMergedChapters = mockk<GetMergedChaptersByMangaId>()
    private val getBookmarked = GetBookmarkedChaptersByMangaId(repository, getManga, getMergedChapters)

    @Test
    fun unknownMangaIsEmpty() = runTest {
        coEvery { getManga.await(7L) } returns null

        getBookmarked.await(7L) shouldBe emptyList()

        coVerify(exactly = 0) { repository.getBookmarkedChaptersByMangaId(any()) }
    }

    @Test
    fun mergedMangaFiltersBookmarks() = runTest {
        coEvery { getManga.await(7L) } returns Manga.create().copy(id = 7L, source = MERGED_SOURCE_ID)
        coEvery {
            getMergedChapters.await(mangaId = 7L, dedupe = true, applyScanlatorFilter = true)
        } returns listOf(bookmarked, plain)

        getBookmarked.await(7L) shouldBe listOf(bookmarked)
    }

    @Test
    fun plainMangaReadsTheStore() = runTest {
        coEvery { getManga.await(7L) } returns Manga.create().copy(id = 7L, source = 3L)
        coEvery { repository.getBookmarkedChaptersByMangaId(7L) } returns listOf(bookmarked)

        getBookmarked.await(7L) shouldBe listOf(bookmarked)
    }

    @Test
    fun storeFailureIsEmpty() = runTest {
        coEvery { getManga.await(7L) } returns Manga.create().copy(id = 7L, source = 3L)
        coEvery { repository.getBookmarkedChaptersByMangaId(7L) } throws IOException("store down")

        getBookmarked.await(7L) shouldBe emptyList()
    }
}
