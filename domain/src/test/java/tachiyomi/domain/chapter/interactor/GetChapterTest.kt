package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import java.io.IOException

internal class GetChapterTest {

    private val chapter = Chapter.create().copy(id = 3L, mangaId = 7L, url = "/c3")
    private val repository = mockk<ChapterRepository>()
    private val getChapter = GetChapter(repository)

    @Test
    fun byIdReturnsTheRow() = runTest {
        coEvery { repository.getChapterById(3L) } returns chapter

        getChapter.await(3L) shouldBe chapter
    }

    @Test
    fun byIdReturnsNullForUnknown() = runTest {
        coEvery { repository.getChapterById(4L) } returns null

        getChapter.await(4L) shouldBe null
    }

    @Test
    fun byIdSwallowsStoreFailure() = runTest {
        coEvery { repository.getChapterById(3L) } throws IOException("store down")

        getChapter.await(3L) shouldBe null
    }

    @Test
    fun byUrlReturnsTheRow() = runTest {
        coEvery { repository.getChapterByUrlAndMangaId("/c3", 7L) } returns chapter

        getChapter.await("/c3", 7L) shouldBe chapter
    }

    @Test
    fun byUrlSwallowsStoreFailure() = runTest {
        coEvery { repository.getChapterByUrlAndMangaId("/c3", 7L) } throws IllegalStateException("store down")

        getChapter.await("/c3", 7L) shouldBe null
    }
}
