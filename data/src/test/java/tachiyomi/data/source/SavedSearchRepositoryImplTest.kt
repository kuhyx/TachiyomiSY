package tachiyomi.data.source

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.domain.source.model.SavedSearch

/** A saved search for [source]; the id is ignored on insert. */
internal fun savedSearch(source: Long = 1L, name: String = "Search", query: String? = "q"): SavedSearch =
    SavedSearch(id = 0L, source = source, name = name, query = query, filtersJson = """{"f":1}""")

internal class SavedSearchRepositoryImplTest {
    private val repository = SavedSearchRepositoryImpl(inMemoryDatabase())

    @Test
    fun insertThenGetById() = runTest {
        val id = repository.insert(savedSearch())

        repository.getById(id) shouldBe savedSearch().copy(id = id)
    }

    @Test
    fun getByIdMissingIsNull() = runTest {
        repository.getById(404L) shouldBe null
    }

    @Test
    fun nullableColumnsRoundTrip() = runTest {
        val id = repository.insert(savedSearch(query = null).copy(filtersJson = null))

        val read = repository.getById(id)

        read?.query shouldBe null
        read?.filtersJson shouldBe null
    }

    @Test
    fun getBySourceIdFiltersBySource() = runTest {
        repository.insert(savedSearch(source = 1L, name = "A"))
        repository.insert(savedSearch(source = 2L, name = "B"))
        repository.insert(savedSearch(source = 1L, name = "C"))

        repository.getBySourceId(1L).map { it.name } shouldBe listOf("A", "C")
        repository.getBySourceId(3L).shouldBeEmpty()
    }

    @Test
    fun getBySourceIdAsFlowEmits() = runTest {
        repository.insert(savedSearch(source = 1L, name = "A"))

        repository.getBySourceIdAsFlow(1L).first().map { it.name } shouldBe listOf("A")
        repository.getBySourceIdAsFlow(2L).first().shouldBeEmpty()
    }

    @Test
    fun deleteRemovesOnlyThatRow() = runTest {
        val first = repository.insert(savedSearch(name = "A"))
        repository.insert(savedSearch(name = "B"))

        repository.delete(first)

        repository.getBySourceId(1L).map { it.name } shouldBe listOf("B")
    }

    @Test
    fun insertAllStoresEveryEntry() = runTest {
        repository.insertAll(listOf(savedSearch(name = "A"), savedSearch(name = "B", query = null)))

        repository.getBySourceId(1L).map { it.name to it.query } shouldBe listOf("A" to "q", "B" to null)
    }

    @Test
    fun insertAllOfNothingIsNoop() = runTest {
        repository.insertAll(emptyList())

        repository.getBySourceId(1L).shouldBeEmpty()
    }
}
