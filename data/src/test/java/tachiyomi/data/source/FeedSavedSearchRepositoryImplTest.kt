package tachiyomi.data.source

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.domain.source.model.FeedSavedSearch

/** A feed entry for [source]; the id is ignored on insert. */
internal fun feed(source: Long = 1L, savedSearch: Long? = null, global: Boolean = false): FeedSavedSearch =
    FeedSavedSearch(id = 0L, source = source, savedSearch = savedSearch, global = global)

internal class FeedSavedSearchRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = FeedSavedSearchRepositoryImpl(database)
    private val savedSearches = SavedSearchRepositoryImpl(database)

    @Test
    fun insertReturnsIdAndReadsBack() = runTest {
        val searchId = savedSearches.insert(savedSearch())

        val id = repository.insert(feed(savedSearch = searchId))

        repository.getBySourceId(1L) shouldBe listOf(feed(savedSearch = searchId).copy(id = id))
    }

    @Test
    fun insertWithoutSavedSearch() = runTest {
        val id = repository.insert(feed(global = true))

        repository.getGlobal() shouldBe listOf(feed(global = true).copy(id = id))
    }

    @Test
    fun deleteRemovesTheEntry() = runTest {
        val id = repository.insert(feed(global = true))
        repository.insert(feed(global = true))

        repository.delete(id)

        repository.countGlobal() shouldBe 1L
    }

    @Test
    fun insertAllStoresEveryEntry() = runTest {
        repository.insertAll(listOf(feed(source = 1L), feed(source = 2L), feed(source = 1L, global = true)))

        repository.getBySourceId(1L).map { it.source } shouldBe listOf(1L)
        repository.getBySourceId(2L).map { it.source } shouldBe listOf(2L)
        repository.countGlobal() shouldBe 1L
    }

    @Test
    fun insertAllOfNothingIsNoop() = runTest {
        repository.insertAll(emptyList())

        repository.countGlobal() shouldBe 0L
    }

    @Test
    fun delegatedReadsWork() = runTest {
        val searchId = savedSearches.insert(savedSearch(name = "S"))
        repository.insert(feed(savedSearch = searchId, global = true))
        repository.insert(feed(source = 7L, savedSearch = searchId))

        repository.getGlobalAsFlow().first().map { it.savedSearch } shouldBe listOf(searchId)
        repository.getGlobalFeedSavedSearch().map { it.name } shouldBe listOf("S")
        repository.getBySourceIdAsFlow(7L).first().map { it.source } shouldBe listOf(7L)
        repository.getBySourceIdFeedSavedSearch(7L).map { it.name } shouldBe listOf("S")
        repository.countBySourceId(7L) shouldBe 1L
        repository.countBySourceId(8L) shouldBe 0L
        repository.getBySourceIdFeedSavedSearch(8L).shouldBeEmpty()
    }
}
