package tachiyomi.domain.chapter.interactor

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.repository.ChapterRepository

internal class DeleteChaptersTest {

    private val repository = mockk<ChapterRepository>()

    @Test
    fun removesTheGivenIds() = runTest {
        coJustRun { repository.removeChaptersWithIds(listOf(1L, 2L)) }

        DeleteChapters(repository).await(listOf(1L, 2L))

        coVerify(exactly = 1) { repository.removeChaptersWithIds(listOf(1L, 2L)) }
    }
}
