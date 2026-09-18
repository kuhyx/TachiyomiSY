package tachiyomi.data.source

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase

internal class FeedSavedSearchReadRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = FeedSavedSearchReadRepositoryImpl(database)
    private val writer = FeedSavedSearchRepositoryImpl(database)
    private var globalSearchId = 0L
    private var sourceSearchId = 0L

    @BeforeEach
    fun seed() = runTest {
        val savedSearches = SavedSearchRepositoryImpl(database)
        globalSearchId = savedSearches.insert(savedSearch(name = "Global"))
        sourceSearchId = savedSearches.insert(savedSearch(source = 2L, name = "Local"))
        writer.insert(feed(source = 1L, savedSearch = globalSearchId, global = true))
        writer.insert(feed(source = 2L, savedSearch = sourceSearchId))
        writer.insert(feed(source = 2L, savedSearch = null))
        writer.insert(feed(source = 3L, savedSearch = null, global = true))
    }

    @Test
    fun getGlobalListsGlobalEntries() = runTest {
        repository.getGlobal().map { it.source } shouldBe listOf(1L, 3L)
        repository.getGlobal().map { it.savedSearch } shouldBe listOf(globalSearchId, null)
    }

    @Test
    fun getGlobalAsFlowEmits() = runTest {
        repository.getGlobalAsFlow().first().map { it.source } shouldBe listOf(1L, 3L)
    }

    @Test
    fun globalFeedSavedSearchJoins() = runTest {
        repository.getGlobalFeedSavedSearch().map { it.name } shouldBe listOf("Global")
    }

    @Test
    fun countGlobalCountsEntries() = runTest {
        repository.countGlobal() shouldBe 2L
    }

    @Test
    fun getBySourceIdExcludesGlobal() = runTest {
        repository.getBySourceId(2L).map { it.savedSearch } shouldBe listOf(sourceSearchId, null)
        repository.getBySourceId(1L).shouldBeEmpty()
    }

    @Test
    fun getBySourceIdAsFlowEmits() = runTest {
        repository.getBySourceIdAsFlow(2L).first().size shouldBe 2
    }

    @Test
    fun sourceFeedSavedSearchJoins() = runTest {
        repository.getBySourceIdFeedSavedSearch(2L).map { it.name } shouldBe listOf("Local")
        repository.getBySourceIdFeedSavedSearch(3L).shouldBeEmpty()
    }

    @Test
    fun countBySourceIdJoins() = runTest {
        repository.countBySourceId(2L) shouldBe 1L
        repository.countBySourceId(3L) shouldBe 0L
    }

    @Test
    fun mappersKeepEveryColumn() = runTest {
        val entry = repository.getBySourceId(2L).first()
        val search = repository.getBySourceIdFeedSavedSearch(2L).single()

        entry shouldBe feed(source = 2L, savedSearch = sourceSearchId).copy(id = entry.id)
        search shouldBe savedSearch(source = 2L, name = "Local").copy(id = sourceSearchId)
    }
}
