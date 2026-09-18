package tachiyomi.domain.category.interactor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences

internal class DeleteCategoryTest {

    private val repository = mockk<CategoryRepository>()
    private val store = InMemoryPreferenceStore()
    private val libraryPreferences = LibraryPreferences(store)
    private val downloadPreferences = DownloadPreferences(store)
    private val interactor = DeleteCategory(repository, libraryPreferences, downloadPreferences)

    @BeforeEach
    fun setUp() {
        coJustRun { repository.delete(any()) }
        coJustRun { repository.updatePartial(any<List<CategoryUpdate>>()) }
        coEvery { repository.getAll() } returns listOf(testCategory(0L), testCategory(1L, order = 4L), testCategory(3L))
    }

    @Test
    fun deletesAndClosesTheOrderGap() = runTest {
        val result = interactor.await(2L)

        result shouldBe DeleteCategory.Result.Success
        coVerify(exactly = 1) { repository.delete(2L) }
        coVerify(exactly = 1) {
            repository.updatePartial(
                listOf(
                    CategoryUpdate(id = 0L, order = 0L),
                    CategoryUpdate(id = 1L, order = 1L),
                    CategoryUpdate(id = 3L, order = 2L),
                ),
            )
        }
    }

    @Test
    fun forgetsTheDefaultCategory() = runTest {
        libraryPreferences.defaultCategory.set(2)

        interactor.await(2L) shouldBe DeleteCategory.Result.Success

        libraryPreferences.defaultCategory.isSet() shouldBe false
        libraryPreferences.defaultCategory.get() shouldBe -1
    }

    @Test
    fun keepsAnotherDefaultCategory() = runTest {
        libraryPreferences.defaultCategory.set(7)

        interactor.await(2L) shouldBe DeleteCategory.Result.Success

        libraryPreferences.defaultCategory.get() shouldBe 7
    }

    @Test
    fun dropsIdFromEveryCategorySet() = runTest {
        libraryPreferences.updateCategories.set(setOf("2", "5"))
        libraryPreferences.updateCategoriesExclude.set(setOf("2"))
        downloadPreferences.removeExcludeCategories.set(setOf("5", "2"))
        downloadPreferences.downloadNewChapterCategories.set(setOf("2"))
        downloadPreferences.downloadNewChapterCategoriesExclude.set(setOf("1", "2"))

        interactor.await(2L) shouldBe DeleteCategory.Result.Success

        libraryPreferences.updateCategories.get() shouldBe setOf("5")
        libraryPreferences.updateCategoriesExclude.get() shouldBe emptySet()
        downloadPreferences.removeExcludeCategories.get() shouldBe setOf("5")
        downloadPreferences.downloadNewChapterCategories.get() shouldBe emptySet()
        downloadPreferences.downloadNewChapterCategoriesExclude.get() shouldBe setOf("1")
    }

    @Test
    fun leavesUnrelatedCategorySets() = runTest {
        libraryPreferences.updateCategories.set(setOf("5"))
        downloadPreferences.downloadNewChapterCategories.set(setOf("7"))

        interactor.await(2L) shouldBe DeleteCategory.Result.Success

        libraryPreferences.updateCategories.get() shouldBe setOf("5")
        libraryPreferences.updateCategoriesExclude.isSet() shouldBe false
        downloadPreferences.downloadNewChapterCategories.get() shouldBe setOf("7")
        downloadPreferences.removeExcludeCategories.isSet() shouldBe false
    }

    @Test
    fun deleteFailureIsInternalError() = runTest {
        val failure = IllegalStateException("db closed")
        coEvery { repository.delete(any()) } throws failure
        libraryPreferences.defaultCategory.set(2)

        val result = interactor.await(2L)

        result shouldBe DeleteCategory.Result.InternalError(failure)
        // The failure happens before the preferences are touched.
        libraryPreferences.defaultCategory.get() shouldBe 2
        coVerify(exactly = 0) { repository.updatePartial(any<List<CategoryUpdate>>()) }
    }

    @Test
    fun reorderFailureIsInternalErr() = runTest {
        val failure = IllegalStateException("db closed")
        coEvery { repository.updatePartial(any<List<CategoryUpdate>>()) } throws failure

        val result = interactor.await(2L)

        result shouldBe DeleteCategory.Result.InternalError(failure)
    }

    @Test
    fun resultsHaveValueSemantics() {
        val failure = IllegalStateException("db closed")
        val error = DeleteCategory.Result.InternalError(failure)
        val (cause) = error
        cause shouldBe failure
        error.copy(error = IllegalArgumentException("other")) shouldNotBe error
        error.hashCode() shouldBe DeleteCategory.Result.InternalError(failure).hashCode()
        error.toString() shouldBe "InternalError(error=$failure)"
        DeleteCategory.Result.Success.toString() shouldBe "Success"
        DeleteCategory.Result.Success shouldNotBe error
    }
}
