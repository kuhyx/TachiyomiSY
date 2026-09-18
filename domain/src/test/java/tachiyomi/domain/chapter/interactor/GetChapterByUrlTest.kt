package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import java.io.IOException

internal class GetChapterByUrlTest {

    private val chapters = listOf(
        Chapter.create().copy(id = 1L, mangaId = 7L, url = "/c"),
        Chapter.create().copy(id = 2L, mangaId = 8L, url = "/c"),
    )
    private val repository = mockk<ChapterRepository>()
    private val getChapterByUrl = GetChapterByUrl(repository)

    @Test
    fun returnsEveryRowAtTheUrl() = runTest {
        coEvery { repository.getChapterByUrl("/c") } returns chapters

        getChapterByUrl.await("/c") shouldBe chapters
    }

    @Test
    fun storeFailureIsEmpty() = runTest {
        coEvery { repository.getChapterByUrl("/c") } throws IOException("store down")

        getChapterByUrl.await("/c") shouldBe emptyList()
    }
}
