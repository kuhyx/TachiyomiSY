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

internal class RenameCategoryTest {

    private val repository = mockk<CategoryRepository>()
    private val interactor = RenameCategory(repository)

    @Test
    fun writesTheNewName() = runTest {
        coJustRun { repository.updatePartial(any<CategoryUpdate>()) }

        val result = interactor.await(3L, "Finished")

        result shouldBe RenameCategory.Result.Success
        coVerify(exactly = 1) { repository.updatePartial(CategoryUpdate(id = 3L, name = "Finished")) }
    }

    @Test
    fun renamesByCategory() = runTest {
        coJustRun { repository.updatePartial(any<CategoryUpdate>()) }

        val result = interactor.await(testCategory(5L), "Dropped")

        result shouldBe RenameCategory.Result.Success
        coVerify(exactly = 1) { repository.updatePartial(CategoryUpdate(id = 5L, name = "Dropped")) }
    }

    @Test
    fun storeFailureIsInternalError() = runTest {
        val failure = IllegalStateException("db closed")
        coEvery { repository.updatePartial(any<CategoryUpdate>()) } throws failure

        val result = interactor.await(3L, "Finished")

        result shouldBe RenameCategory.Result.InternalError(failure)
    }

    @Test
    fun resultsHaveValueSemantics() {
        val failure = IllegalStateException("db closed")
        val error = RenameCategory.Result.InternalError(failure)
        val (cause) = error
        cause shouldBe failure
        error.copy(error = IllegalArgumentException("other")) shouldNotBe error
        error.hashCode() shouldBe RenameCategory.Result.InternalError(failure).hashCode()
        error.toString() shouldBe "InternalError(error=$failure)"
        RenameCategory.Result.Success.toString() shouldBe "Success"
        RenameCategory.Result.Success shouldNotBe error
    }
}
