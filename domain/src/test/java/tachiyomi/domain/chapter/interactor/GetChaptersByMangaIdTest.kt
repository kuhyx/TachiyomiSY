package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import java.io.IOException

internal class GetChaptersByMangaIdTest {

    private val chapters = listOf(Chapter.create().copy(id = 1L, mangaId = 7L))
    private val repository = mockk<ChapterRepository>()
    private val getChapters = GetChaptersByMangaId(repository)

    @Test
    fun unfilteredByDefault() = runTest {
        coEvery { repository.getChapterByMangaId(7L, false) } returns chapters

        getChapters.await(7L) shouldBe chapters
    }

    @Test
    fun scanlatorFilterIsPassedOn() = runTest {
        coEvery { repository.getChapterByMangaId(7L, true) } returns chapters

        getChapters.await(7L, applyScanlatorFilter = true) shouldBe chapters
    }

    @Test
    fun storeFailureIsEmpty() = runTest {
        coEvery { repository.getChapterByMangaId(7L, false) } throws IOException("store down")

        getChapters.await(7L) shouldBe emptyList()
    }
}
