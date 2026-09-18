package tachiyomi.domain.source.interactor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.SavedSearchRepository

internal class SavedSearchInteractorsTest {

    private val repository: SavedSearchRepository = mockk()
    private val savedSearch =
        SavedSearch(id = SEARCH_ID, source = SOURCE_ID, name = "n", query = "q", filtersJson = null)

    @Test
    fun deleteById() = runTest {
        coEvery { repository.delete(SEARCH_ID) } returns Unit

        DeleteSavedSearchById(repository).await(SEARCH_ID)

        coVerify(exactly = 1) { repository.delete(SEARCH_ID) }
    }

    @Test
    fun awaitByIdReturnsRow() = runTest {
        coEvery { repository.getById(SEARCH_ID) } returns savedSearch

        GetSavedSearchById(repository).await(SEARCH_ID) shouldBe savedSearch
    }

    @Test
    fun awaitByIdThrowsWhenMissing() = runTest {
        coEvery { repository.getById(SEARCH_ID) } returns null

        shouldThrow<NullPointerException> { GetSavedSearchById(repository).await(SEARCH_ID) }
    }

    @Test
    fun awaitOrNullReturnsRow() = runTest {
        coEvery { repository.getById(SEARCH_ID) } returns savedSearch

        GetSavedSearchById(repository).awaitOrNull(SEARCH_ID) shouldBe savedSearch
    }

    @Test
    fun awaitOrNullWhenMissing() = runTest {
        coEvery { repository.getById(SEARCH_ID) } returns null

        GetSavedSearchById(repository).awaitOrNull(SEARCH_ID) shouldBe null
    }

    @Test
    fun bySourceIdAwait() = runTest {
        coEvery { repository.getBySourceId(SOURCE_ID) } returns listOf(savedSearch)

        GetSavedSearchBySourceId(repository).await(SOURCE_ID) shouldContainExactly listOf(savedSearch)
    }

    @Test
    fun bySourceIdSubscribe() = runTest {
        every { repository.getBySourceIdAsFlow(SOURCE_ID) } returns flowOf(listOf(savedSearch))

        GetSavedSearchBySourceId(repository).subscribe(SOURCE_ID).first() shouldContainExactly listOf(savedSearch)
    }

    private companion object {
        const val SEARCH_ID = 3L
        const val SOURCE_ID = 4L
    }
}
