package tachiyomi.domain.updates.interactor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.CustomMangaInfoScope
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.model.updatesRow
import tachiyomi.domain.updates.repository.UpdatesRepository
import java.time.Instant

internal class GetUpdatesTest {

    private val repository: UpdatesRepository = mockk()
    private val getUpdates = GetUpdates(repository)
    private val instant = Instant.ofEpochMilli(AFTER)
    private lateinit var rows: List<UpdatesWithRelations>

    @BeforeEach
    fun beforeEach() {
        CustomMangaInfoScope.install()
        rows = listOf(updatesRow(mangaId = CustomMangaInfoScope.PLAIN_MANGA_ID))
    }

    @AfterEach
    fun afterEach() {
        CustomMangaInfoScope.restore()
    }

    @Test
    fun awaitReadsWithLimit() = runTest {
        coEvery { repository.awaitWithRead(true, AFTER, limit = 500) } returns rows

        getUpdates.await(read = true, after = AFTER) shouldContainExactly rows
    }

    @Test
    fun awaitResumesAfterSuspension() = runTest {
        val suspending = SuspendingUpdatesRepository(rows)

        GetUpdates(suspending).await(read = true, after = AFTER) shouldContainExactly rows
        suspending.reads shouldBe listOf(Triple(true, AFTER, 500L))
    }

    @Test
    fun awaitRetriesAfterNpe() = runTest {
        coEvery {
            repository.awaitWithRead(false, AFTER, limit = 500)
        } throws NullPointerException("store") andThen rows

        getUpdates.await(read = false, after = AFTER) shouldContainExactly rows
    }

    @Test
    fun awaitPropagatesOtherErrors() = runTest {
        coEvery { repository.awaitWithRead(false, AFTER, limit = 500) } throws IllegalStateException("store failed")

        shouldThrow<IllegalStateException> { getUpdates.await(read = false, after = AFTER) }
    }

    @Test
    fun subscribeFiltersPassThrough() = runTest {
        every {
            repository.subscribeAll(
                after = AFTER,
                limit = 500,
                unread = true,
                started = null,
                bookmarked = false,
                hideExcludedScanlators = true,
            )
        } returns npeThenRows()

        val result = getUpdates.subscribe(
            instant = instant,
            unread = true,
            started = null,
            bookmarked = false,
            hideExcludedScanlators = true,
        ).first()

        result shouldContainExactly rows
    }

    @Test
    fun subscribePropagatesErrors() = runTest {
        every {
            repository.subscribeAll(
                after = AFTER,
                limit = 500,
                unread = null,
                started = null,
                bookmarked = null,
                hideExcludedScanlators = false,
            )
        } returns flow { throw IllegalStateException("store failed") }

        shouldThrow<IllegalStateException> {
            getUpdates.subscribe(
                instant = instant,
                unread = null,
                started = null,
                bookmarked = null,
                hideExcludedScanlators = false,
            ).first()
        }
    }

    @Test
    fun subscribeWithReadRetries() = runTest {
        every { repository.subscribeWithRead(true, AFTER, limit = 500) } returns npeThenRows()

        getUpdates.subscribe(read = true, after = AFTER).first() shouldContainExactly rows
    }

    private fun npeThenRows(): Flow<List<UpdatesWithRelations>> {
        var attempts = 0
        return flow {
            attempts++
            if (attempts == 1) throw NullPointerException("store")
            emit(rows)
        }
    }

    private companion object {
        const val AFTER = 1_000L
    }
}

/** A store whose one-shot read really suspends before answering, so the caller's resume path runs. */
private class SuspendingUpdatesRepository(
    private val rows: List<UpdatesWithRelations>,
) : UpdatesRepository {
    val reads = mutableListOf<Triple<Boolean, Long, Long>>()

    override suspend fun awaitWithRead(read: Boolean, after: Long, limit: Long): List<UpdatesWithRelations> {
        reads += Triple(read, after, limit)
        yield()
        return rows
    }

    override fun subscribeAll(
        after: Long,
        limit: Long,
        unread: Boolean?,
        started: Boolean?,
        bookmarked: Boolean?,
        hideExcludedScanlators: Boolean,
    ): Flow<List<UpdatesWithRelations>> = flowOf(rows)

    override fun subscribeWithRead(read: Boolean, after: Long, limit: Long): Flow<List<UpdatesWithRelations>> =
        flowOf(rows)
}
