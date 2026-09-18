package tachiyomi.domain.category.interactor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository

internal class ReorderCategoryTest {

    private val repository = mockk<CategoryRepository>()
    private val interactor = ReorderCategory(repository)

    @BeforeEach
    fun setUp() {
        // The system category (id 0) is listed by the store but never takes part in the reorder.
        coEvery { repository.getAll() } returns listOf(
            testCategory(0L),
            testCategory(1L),
            testCategory(2L),
            testCategory(3L),
        )
        coJustRun { repository.updatePartial(any<List<CategoryUpdate>>()) }
    }

    @Test
    fun movesTheCategoryDown() = runTest {
        val result = interactor.await(testCategory(1L), 2)

        result shouldBe ReorderCategory.Result.Success
        coVerify(exactly = 1) {
            repository.updatePartial(
                listOf(
                    CategoryUpdate(id = 2L, order = 0L),
                    CategoryUpdate(id = 3L, order = 1L),
                    CategoryUpdate(id = 1L, order = 2L),
                ),
            )
        }
    }

    @Test
    fun movesTheCategoryUp() = runTest {
        val result = interactor.await(testCategory(3L), 0)

        result shouldBe ReorderCategory.Result.Success
        coVerify(exactly = 1) {
            repository.updatePartial(
                listOf(
                    CategoryUpdate(id = 3L, order = 0L),
                    CategoryUpdate(id = 1L, order = 1L),
                    CategoryUpdate(id = 2L, order = 2L),
                ),
            )
        }
    }

    @Test
    fun unknownCategoryIsUnchanged() = runTest {
        val result = interactor.await(testCategory(9L), 0)

        result shouldBe ReorderCategory.Result.Unchanged
        coVerify(exactly = 0) { repository.updatePartial(any<List<CategoryUpdate>>()) }
    }

    @Test
    fun systemCategoryIsUnchanged() = runTest {
        interactor.await(testCategory(0L), 1) shouldBe ReorderCategory.Result.Unchanged
    }

    @Test
    fun outOfRangeIndexIsInternalErr() = runTest {
        val result = interactor.await(testCategory(1L), 5)

        result.shouldBeInstanceOf<ReorderCategory.Result.InternalError>()
            .error
            .shouldBeInstanceOf<IndexOutOfBoundsException>()
        coVerify(exactly = 0) { repository.updatePartial(any<List<CategoryUpdate>>()) }
    }

    @Test
    fun storeFailureIsInternalError() = runTest {
        val failure = IllegalStateException("db closed")
        coEvery { repository.updatePartial(any<List<CategoryUpdate>>()) } throws failure

        interactor.await(testCategory(2L), 0) shouldBe ReorderCategory.Result.InternalError(failure)
    }

    @Test
    fun resultsHaveValueSemantics() {
        val failure = IllegalStateException("db closed")
        val error = ReorderCategory.Result.InternalError(failure)
        val (cause) = error
        cause shouldBe failure
        error.copy(error = IllegalArgumentException("other")) shouldNotBe error
        error.hashCode() shouldBe ReorderCategory.Result.InternalError(failure).hashCode()
        error.toString() shouldBe "InternalError(error=$failure)"
        ReorderCategory.Result.Success.toString() shouldBe "Success"
        ReorderCategory.Result.Unchanged.toString() shouldBe "Unchanged"
        ReorderCategory.Result.Success shouldNotBe ReorderCategory.Result.Unchanged
    }
}
