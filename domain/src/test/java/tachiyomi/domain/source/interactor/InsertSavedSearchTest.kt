package tachiyomi.domain.source.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.SavedSearchRepository

internal class InsertSavedSearchTest {

    private val repository: SavedSearchRepository = mockk()
    private val insert = InsertSavedSearch(repository)
    private val savedSearch = SavedSearch(id = 1L, source = 2L, name = "name", query = "q", filtersJson = "[]")

    @Test
    fun awaitReturnsInsertedId() = runTest {
        coEvery { repository.insert(savedSearch) } returns 10L

        insert.await(savedSearch) shouldBe 10L
    }

    @Test
    fun awaitNullOnStoreFailure() = runTest {
        coEvery { repository.insert(savedSearch) } throws IllegalStateException("store failed")

        insert.await(savedSearch) shouldBe null
    }

    @Test
    fun awaitAllInsertsEvery() = runTest {
        coEvery { repository.insertAll(listOf(savedSearch)) } returns Unit

        insert.awaitAll(listOf(savedSearch))

        coVerify(exactly = 1) { repository.insertAll(listOf(savedSearch)) }
    }

    @Test
    fun awaitAllSwallowsFailure() = runTest {
        coEvery { repository.insertAll(listOf(savedSearch)) } throws IllegalStateException("store failed")

        insert.awaitAll(listOf(savedSearch))

        coVerify(exactly = 1) { repository.insertAll(listOf(savedSearch)) }
    }
}
