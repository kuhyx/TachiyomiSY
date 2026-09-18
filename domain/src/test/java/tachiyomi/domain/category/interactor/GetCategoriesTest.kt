package tachiyomi.domain.category.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.repository.CategoryRepository

internal class GetCategoriesTest {

    private val repository = mockk<CategoryRepository>()
    private val interactor = GetCategories(repository)
    private val categories = listOf(testCategory(0L), testCategory(1L))

    @Test
    fun subscribeStreamsAll() = runTest {
        every { repository.getAllAsFlow() } returns flowOf(categories)

        interactor.subscribe().first() shouldBe categories
    }

    @Test
    fun subscribeStreamsForManga() = runTest {
        every { repository.getCategoriesByMangaIdAsFlow(7L) } returns flowOf(categories.drop(1))

        interactor.subscribe(7L).first() shouldBe listOf(testCategory(1L))
    }

    @Test
    fun awaitReadsEveryCategory() = runTest {
        coEvery { repository.getAll() } returns categories

        interactor.await() shouldBe categories
    }

    @Test
    fun awaitReadsMangaCategories() = runTest {
        coEvery { repository.getCategoriesByMangaId(7L) } returns emptyList()

        interactor.await(7L) shouldBe emptyList()
    }
}
