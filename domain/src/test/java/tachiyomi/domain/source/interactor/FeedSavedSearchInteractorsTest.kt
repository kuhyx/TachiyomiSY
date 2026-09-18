package tachiyomi.domain.source.interactor

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
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchRepository

internal class FeedSavedSearchInteractorsTest {

    private val repository: FeedSavedSearchRepository = mockk()
    private val feed = FeedSavedSearch(id = 1L, source = SOURCE_ID, savedSearch = null, global = false)
    private val savedSearch = SavedSearch(id = 2L, source = SOURCE_ID, name = "name", query = null, filtersJson = null)

    @Test
    fun countBySourceId() = runTest {
        coEvery { repository.countBySourceId(SOURCE_ID) } returns 3L

        CountFeedSavedSearchBySourceId(repository).await(SOURCE_ID) shouldBe 3L
    }

    @Test
    fun countGlobal() = runTest {
        coEvery { repository.countGlobal() } returns 4L

        CountFeedSavedSearchGlobal(repository).await() shouldBe 4L
    }

    @Test
    fun deleteById() = runTest {
        coEvery { repository.delete(9L) } returns Unit

        DeleteFeedSavedSearchById(repository).await(9L)

        coVerify(exactly = 1) { repository.delete(9L) }
    }

    @Test
    fun bySourceIdAwait() = runTest {
        coEvery { repository.getBySourceId(SOURCE_ID) } returns listOf(feed)

        GetFeedSavedSearchBySourceId(repository).await(SOURCE_ID) shouldContainExactly listOf(feed)
    }

    @Test
    fun bySourceIdSubscribe() = runTest {
        every { repository.getBySourceIdAsFlow(SOURCE_ID) } returns flowOf(listOf(feed))

        GetFeedSavedSearchBySourceId(repository).subscribe(SOURCE_ID).first() shouldContainExactly listOf(feed)
    }

    @Test
    fun globalAwait() = runTest {
        coEvery { repository.getGlobal() } returns listOf(feed)

        GetFeedSavedSearchGlobal(repository).await() shouldContainExactly listOf(feed)
    }

    @Test
    fun globalSubscribe() = runTest {
        every { repository.getGlobalAsFlow() } returns flowOf(listOf(feed))

        GetFeedSavedSearchGlobal(repository).subscribe().first() shouldContainExactly listOf(feed)
    }

    @Test
    fun savedSearchesOfSourceFeed() = runTest {
        coEvery { repository.getBySourceIdFeedSavedSearch(SOURCE_ID) } returns listOf(savedSearch)

        GetSavedSearchBySourceIdFeed(repository).await(SOURCE_ID) shouldContainExactly listOf(savedSearch)
    }

    @Test
    fun savedSearchesOfGlobalFeed() = runTest {
        coEvery { repository.getGlobalFeedSavedSearch() } returns listOf(savedSearch)

        GetSavedSearchGlobalFeed(repository).await() shouldContainExactly listOf(savedSearch)
    }

    private companion object {
        const val SOURCE_ID = 5L
    }
}
