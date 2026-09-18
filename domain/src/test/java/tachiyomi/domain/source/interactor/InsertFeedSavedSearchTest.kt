package tachiyomi.domain.source.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchRepository

internal class InsertFeedSavedSearchTest {

    private val repository: FeedSavedSearchRepository = mockk()
    private val insert = InsertFeedSavedSearch(repository)
    private val feed = FeedSavedSearch(id = 1L, source = 2L, savedSearch = 3L, global = true)

    @Test
    fun awaitReturnsInsertedId() = runTest {
        coEvery { repository.insert(feed) } returns 10L

        insert.await(feed) shouldBe 10L
    }

    @Test
    fun awaitNullOnStoreFailure() = runTest {
        coEvery { repository.insert(feed) } throws IllegalStateException("store failed")

        insert.await(feed) shouldBe null
    }

    @Test
    fun awaitAllInsertsEvery() = runTest {
        coEvery { repository.insertAll(listOf(feed)) } returns Unit

        insert.awaitAll(listOf(feed))

        coVerify(exactly = 1) { repository.insertAll(listOf(feed)) }
    }

    @Test
    fun awaitAllSwallowsFailure() = runTest {
        coEvery { repository.insertAll(listOf(feed)) } throws IllegalStateException("store failed")

        insert.awaitAll(listOf(feed))

        coVerify(exactly = 1) { repository.insertAll(listOf(feed)) }
    }
}
