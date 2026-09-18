package tachiyomi.domain.category.interactor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository

internal class UpdateCategoryTest {

    private val repository = mockk<CategoryRepository>()
    private val interactor = UpdateCategory(repository)
    private val payload = CategoryUpdate(id = 2L, flags = 0b100L)

    @Test
    fun writesThePayload() = runTest {
        coJustRun { repository.updatePartial(any<CategoryUpdate>()) }

        val result = interactor.await(payload)

        result shouldBe UpdateCategory.Result.Success
        coVerify(exactly = 1) { repository.updatePartial(payload) }
    }

    @Test
    fun storeFailureIsError() = runTest {
        val failure = IllegalStateException("db closed")
        coEvery { repository.updatePartial(any<CategoryUpdate>()) } throws failure

        val result = interactor.await(payload)

        result shouldBe UpdateCategory.Result.Error(failure)
    }

    @Test
    fun resultsHaveValueSemantics() {
        val failure = IllegalStateException("db closed")
        val error = UpdateCategory.Result.Error(failure)
        val (cause) = error
        cause shouldBe failure
        error.copy(error = IllegalArgumentException("other")) shouldNotBe error
        error.hashCode() shouldBe UpdateCategory.Result.Error(failure).hashCode()
        error.toString() shouldBe "Error(error=$failure)"
        UpdateCategory.Result.Success.toString() shouldBe "Success"
        UpdateCategory.Result.Success shouldNotBe error
    }
}
