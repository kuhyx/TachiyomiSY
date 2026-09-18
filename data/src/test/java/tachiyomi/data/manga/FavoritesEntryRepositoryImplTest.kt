package tachiyomi.data.manga

import app.cash.sqldelight.async.coroutines.awaitAsList
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.domain.manga.model.FavoriteEntry
import tachiyomi.domain.manga.model.FavoriteEntryAlternative

internal class FavoritesEntryRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = FavoritesEntryRepositoryImpl(database)
    private val entry = FavoriteEntry(title = "One", gid = "1", token = "a", category = 3)
    private val alternative = FavoriteEntryAlternative(otherGid = "2", otherToken = "b", gid = "1", token = "a")

    @Test
    fun insertAllThenSelectAll() = runTest {
        repository.insertAll(emptyList())
        repository.selectAll() shouldBe emptyList()

        repository.insertAll(listOf(entry, entry.copy(gid = "9", token = "z", title = "Two", category = 0)))

        val stored = repository.selectAll()
        stored.map { it.gid } shouldBe listOf("1", "9")
        stored.first() shouldBe entry
        stored.last().category shouldBe 0
        stored.last().otherGid shouldBe null
    }

    @Test
    fun deleteAllEmptiesTable() = runTest {
        repository.insertAll(listOf(entry))

        repository.deleteAll()

        repository.selectAll() shouldBe emptyList()
    }

    @Test
    fun addAlternativeJoinsSelectAll() = runTest {
        repository.insertAll(listOf(entry))

        repository.addAlternative(alternative)

        val stored = repository.selectAll().single()
        stored.gid shouldBe "1"
        stored.token shouldBe "a"
        stored.otherGid shouldBe "2"
        stored.otherToken shouldBe "b"
    }

    @Test
    fun addAlternativeMatchesOther() = runTest {
        repository.insertAll(listOf(entry.copy(gid = "2", token = "b")))

        repository.addAlternative(alternative)

        val stored = repository.selectAll().single()
        stored.gid shouldBe "1"
        stored.token shouldBe "a"
        stored.otherGid shouldBe "2"
    }

    @Test
    fun addAlternativeSwallowsError() = runTest {
        repository.addAlternative(alternative)

        repository.addAlternative(alternative)

        database.eh_favoritesQueries.selectAll().awaitAsList() shouldBe emptyList()
    }
}
