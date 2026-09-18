package tachiyomi.domain.category.interactor

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.repository.MangaRepository

internal class SetMangaCategoriesTest {

    private val repository = mockk<MangaRepository>()
    private val interactor = SetMangaCategories(repository)

    @Test
    fun linksTheMangaToTheCategories() = runTest {
        coJustRun { repository.setMangaCategories(any(), any()) }

        interactor.await(4L, listOf(1L, 2L))

        coVerify(exactly = 1) { repository.setMangaCategories(4L, listOf(1L, 2L)) }
    }

    @Test
    fun storeFailureIsSwallowed() = runTest {
        coEvery { repository.setMangaCategories(any(), any()) } throws IllegalStateException("db closed")

        interactor.await(4L, emptyList())

        coVerify(exactly = 1) { repository.setMangaCategories(4L, emptyList()) }
    }
}
