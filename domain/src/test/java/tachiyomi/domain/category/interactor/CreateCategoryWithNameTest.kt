package tachiyomi.domain.category.interactor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences

internal class CreateCategoryWithNameTest {

    private val repository = mockk<CategoryRepository>()
    private val preferences = LibraryPreferences(InMemoryPreferenceStore())
    private val interactor = CreateCategoryWithName(repository, preferences)

    @Test
    fun firstCategoryGetsOrderZero() = runTest {
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.insert(any()) } returns 1L

        val result = interactor.await("New")

        val expected = Category(id = 0L, name = "New", order = 0L, flags = LibrarySort.default.flag)
        result shouldBe CreateCategoryWithName.Result.Success(expected)
        coVerify(exactly = 1) { repository.insert(expected) }
    }

    @Test
    fun appendsAfterTheHighestOrder() = runTest {
        val existing = listOf(testCategory(1L, order = 2L), testCategory(2L, order = 1L), testCategory(3L))
        coEvery { repository.getAll() } returns existing
        coEvery { repository.insert(any()) } returns 4L

        val result = interactor.await("Fourth")

        result.shouldBeInstanceOf<CreateCategoryWithName.Result.Success>().category.order shouldBe 4L
    }

    @Test
    fun singleCategoryGetsOrderOne() = runTest {
        coEvery { repository.getAll() } returns listOf(testCategory(1L, order = 0L))
        coEvery { repository.insert(any()) } returns 2L

        val result = interactor.await("Second")

        result.shouldBeInstanceOf<CreateCategoryWithName.Result.Success>().category.order shouldBe 1L
    }

    @Test
    fun flagsFollowTheLibrarySort() = runTest {
        preferences.sortingMode.set(LibrarySort(LibrarySort.Type.Random, LibrarySort.Direction.Descending))
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.insert(any()) } returns null

        val result = interactor.await("Shuffled")

        result.shouldBeInstanceOf<CreateCategoryWithName.Result.Success>().category.flags shouldBe 0b00111100L
    }

    @Test
    fun insertFailureIsInternalError() = runTest {
        val failure = IllegalStateException("db closed")
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.insert(any()) } throws failure

        val result = interactor.await("Broken")

        result shouldBe CreateCategoryWithName.Result.InternalError(failure)
    }

    @Test
    fun successIsADataClass() {
        val success = CreateCategoryWithName.Result.Success(testCategory(1L))
        val (category) = success
        category shouldBe testCategory(1L)
        success.copy(category = testCategory(2L)) shouldNotBe success
        success.hashCode() shouldBe CreateCategoryWithName.Result.Success(testCategory(1L)).hashCode()
        success.toString() shouldBe "Success(category=${testCategory(1L)})"
    }

    @Test
    fun internalErrorIsADataClass() {
        val failure = IllegalStateException("db closed")
        val error = CreateCategoryWithName.Result.InternalError(failure)
        val (cause) = error
        cause shouldBe failure
        error.copy(error = IllegalArgumentException("other")) shouldNotBe error
        error.hashCode() shouldBe CreateCategoryWithName.Result.InternalError(failure).hashCode()
        error.toString() shouldBe "InternalError(error=$failure)"
    }
}
