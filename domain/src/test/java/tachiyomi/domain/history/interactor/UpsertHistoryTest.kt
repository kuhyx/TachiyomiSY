package tachiyomi.domain.history.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.repository.HistoryRepository
import java.util.Date

internal class UpsertHistoryTest {

    private val repository: HistoryRepository = mockk()
    private val upsertHistory = UpsertHistory(repository)
    private val update = HistoryUpdate(chapterId = 1L, readAt = Date(0L), sessionReadDuration = 10L)

    @Test
    fun awaitStoresUpdate() = runTest {
        coEvery { repository.upsertHistory(update) } returns Unit

        upsertHistory.await(update)

        coVerify(exactly = 1) { repository.upsertHistory(update) }
    }

    @Test
    fun awaitSwallowsStoreFailure() = runTest {
        coEvery { repository.upsertHistory(update) } throws IllegalStateException("store failed")

        upsertHistory.await(update)

        coVerify(exactly = 1) { repository.upsertHistory(update) }
    }

    @Test
    fun awaitAllStoresUpdates() = runTest {
        coEvery { repository.upsertAllHistory(listOf(update)) } returns Unit

        upsertHistory.awaitAll(listOf(update))

        coVerify(exactly = 1) { repository.upsertAllHistory(listOf(update)) }
    }

    @Test
    fun awaitAllSwallowsFailure() = runTest {
        coEvery { repository.upsertAllHistory(listOf(update)) } throws IllegalStateException("store failed")

        upsertHistory.awaitAll(listOf(update))

        coVerify(exactly = 1) { repository.upsertAllHistory(listOf(update)) }
    }
}
