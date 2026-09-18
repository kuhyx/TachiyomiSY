package tachiyomi.domain.manga.interactor

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.FavoriteEntry
import tachiyomi.domain.manga.model.FavoriteEntryAlternative
import tachiyomi.domain.manga.repository.FavoritesEntryRepository

internal class FavoriteEntryInteractorsTest {

    private val entry = FavoriteEntry(title = "Title", gid = "1", token = "t")
    private val repository = mockk<FavoritesEntryRepository>()

    @Test
    fun deleteAllDelegates() = runTest {
        coEvery { repository.deleteAll() } returns Unit

        DeleteFavoriteEntries(repository).await()

        coVerify(exactly = 1) { repository.deleteAll() }
    }

    @Test
    fun selectAllDelegates() = runTest {
        coEvery { repository.selectAll() } returns listOf(entry)

        GetFavoriteEntries(repository).await() shouldContainExactly listOf(entry)
    }

    @Test
    fun insertAllDelegates() = runTest {
        val batch = listOf(entry, entry.copy(gid = "2"))
        coEvery { repository.insertAll(batch) } returns Unit

        InsertFavoriteEntries(repository).await(batch)

        coVerify(exactly = 1) { repository.insertAll(batch) }
    }

    @Test
    fun addAlternativeDelegates() = runTest {
        val alternative = FavoriteEntryAlternative(otherGid = "9", otherToken = "o", gid = "1", token = "t")
        coEvery { repository.addAlternative(alternative) } returns Unit

        InsertFavoriteEntryAlternative(repository).await(alternative)

        coVerify(exactly = 1) { repository.addAlternative(alternative) }
    }
}
