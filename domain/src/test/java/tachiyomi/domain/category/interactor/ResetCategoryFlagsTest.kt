package tachiyomi.domain.category.interactor

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences

internal class ResetCategoryFlagsTest {

    private val repository = mockk<CategoryRepository>()
    private val preferences = LibraryPreferences(InMemoryPreferenceStore())
    private val interactor = ResetCategoryFlags(preferences, repository)

    @Test
    fun writesTheDefaultSortToAll() = runTest {
        coJustRun { repository.updateAllFlags(any()) }

        interactor.await()

        coVerify(exactly = 1) { repository.updateAllFlags(0b01000000L) }
    }

    @Test
    fun writesTheStoredSortToAll() = runTest {
        coJustRun { repository.updateAllFlags(any()) }
        preferences.sortingMode.set(LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Descending))

        interactor.await()

        coVerify(exactly = 1) { repository.updateAllFlags(0b00011100L) }
    }
}
