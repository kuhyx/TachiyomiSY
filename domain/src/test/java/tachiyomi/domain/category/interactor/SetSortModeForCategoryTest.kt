package tachiyomi.domain.category.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences

internal class SetSortModeForCategoryTest {

    private val repository = mockk<CategoryRepository>()
    private val preferences = LibraryPreferences(InMemoryPreferenceStore())
    private val interactor = SetSortModeForCategory(preferences, repository)
    private val lastRead = LibrarySort(LibrarySort.Type.LastRead, LibrarySort.Direction.Descending)

    // Typed nulls: a bare null is ambiguous between the id and the category overloads.
    private val noCategoryId: Long? = null
    private val noCategory: Category? = null

    @BeforeEach
    fun setUp() {
        coJustRun { repository.updatePartial(any<CategoryUpdate>()) }
        coJustRun { repository.updateAllFlags(any()) }
        coEvery { repository.get(any()) } returns null
        coEvery { repository.get(3L) } returns testCategory(3L, flags = 0b01000010L)
    }

    @Test
    fun groupedLibrarySetsPrefOnly() = runTest {
        preferences.groupLibraryBy.set(LibraryGroup.BY_SOURCE)
        preferences.categorizedDisplaySettings.set(true)

        interactor.await(3L, LibrarySort.Type.LastRead, LibrarySort.Direction.Descending)

        preferences.sortingMode.get() shouldBe lastRead
        coVerify(exactly = 0) { repository.get(any()) }
        coVerify(exactly = 0) { repository.updatePartial(any<CategoryUpdate>()) }
        coVerify(exactly = 0) { repository.updateAllFlags(any()) }
    }

    @Test
    fun perCategorySortKeepsBits() = runTest {
        preferences.categorizedDisplaySettings.set(true)

        interactor.await(3L, LibrarySort.Type.LastRead, LibrarySort.Direction.Descending)

        // Bit 1 is outside the sort mask and survives; bits 2-6 are replaced.
        coVerify(exactly = 1) { repository.updatePartial(CategoryUpdate(id = 3L, flags = 0b00000110L)) }
        coVerify(exactly = 0) { repository.updateAllFlags(any()) }
        preferences.sortingMode.get() shouldBe LibrarySort.default
    }

    @Test
    fun globalSortWhenNotCategorized() = runTest {
        interactor.await(3L, LibrarySort.Type.LastRead, LibrarySort.Direction.Ascending)

        preferences.sortingMode.get() shouldBe lastRead.copy(direction = LibrarySort.Direction.Ascending)
        coVerify(exactly = 1) { repository.updateAllFlags(0b01000110L) }
        coVerify(exactly = 0) { repository.updatePartial(any<CategoryUpdate>()) }
    }

    @Test
    fun globalSortWhenNoSuchCategory() = runTest {
        preferences.categorizedDisplaySettings.set(true)

        interactor.await(9L, LibrarySort.Type.LastRead, LibrarySort.Direction.Descending)

        preferences.sortingMode.get() shouldBe lastRead
        coVerify(exactly = 1) { repository.updateAllFlags(0b00000100L) }
    }

    @Test
    fun globalSortWhenNoCategoryId() = runTest {
        preferences.categorizedDisplaySettings.set(true)

        interactor.await(noCategoryId, LibrarySort.Type.LastRead, LibrarySort.Direction.Descending)

        preferences.sortingMode.get() shouldBe lastRead
        coVerify(exactly = 0) { repository.get(any()) }
        coVerify(exactly = 1) { repository.updateAllFlags(0b00000100L) }
    }

    @Test
    fun randomSortReseeds() = runTest {
        preferences.randomSortSeed.isSet() shouldBe false

        interactor.await(noCategoryId, LibrarySort.Type.Random, LibrarySort.Direction.Ascending)

        preferences.randomSortSeed.isSet() shouldBe true
        coVerify(exactly = 1) { repository.updateAllFlags(0b01111100L) }
    }

    @Test
    fun otherSortsKeepTheSeed() = runTest {
        interactor.await(noCategoryId, LibrarySort.Type.Alphabetical, LibrarySort.Direction.Ascending)

        preferences.randomSortSeed.isSet() shouldBe false
    }

    @Test
    fun categoryOverloadUsesItsId() = runTest {
        preferences.categorizedDisplaySettings.set(true)

        interactor.await(testCategory(3L), LibrarySort.Type.LastRead, LibrarySort.Direction.Descending)

        coVerify(exactly = 1) { repository.updatePartial(CategoryUpdate(id = 3L, flags = 0b00000110L)) }
    }

    @Test
    fun nullCategoryOverloadIsGlobal() = runTest {
        preferences.categorizedDisplaySettings.set(true)

        interactor.await(noCategory, LibrarySort.Type.LastRead, LibrarySort.Direction.Descending)

        preferences.sortingMode.get() shouldBe lastRead
        coVerify(exactly = 1) { repository.updateAllFlags(0b00000100L) }
    }
}
