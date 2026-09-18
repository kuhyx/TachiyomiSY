package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import java.io.IOException

internal class GetChapterByUrlAndMangaIdTest {

    private val chapter = Chapter.create().copy(id = 1L, mangaId = 7L, url = "/c")
    private val repository = mockk<ChapterRepository>()
    private val getChapter = GetChapterByUrlAndMangaId(repository)

    @Test
    fun returnsTheRow() = runTest {
        coEvery { repository.getChapterByUrlAndMangaId("/c", 7L) } returns chapter

        getChapter.await("/c", 7L) shouldBe chapter
    }

    @Test
    fun returnsNullForUnknown() = runTest {
        coEvery { repository.getChapterByUrlAndMangaId("/c", 8L) } returns null

        getChapter.await("/c", 8L) shouldBe null
    }

    @Test
    fun storeFailureIsNull() = runTest {
        coEvery { repository.getChapterByUrlAndMangaId("/c", 7L) } throws IOException("store down")

        getChapter.await("/c", 7L) shouldBe null
    }
}
