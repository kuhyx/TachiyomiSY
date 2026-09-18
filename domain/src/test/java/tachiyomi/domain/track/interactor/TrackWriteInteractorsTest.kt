package tachiyomi.domain.track.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.model.track
import tachiyomi.domain.track.repository.TrackRepository

internal class TrackWriteInteractorsTest {

    private val repository: TrackRepository = mockk()
    private val row = track()

    @Test
    fun deleteRemovesLink() = runTest {
        coEvery { repository.delete(10L, 1L) } returns Unit

        DeleteTrack(repository).await(10L, 1L)

        coVerify(exactly = 1) { repository.delete(10L, 1L) }
    }

    @Test
    fun deleteSwallowsFailure() = runTest {
        coEvery { repository.delete(10L, 1L) } throws IllegalStateException("store failed")

        DeleteTrack(repository).await(10L, 1L)

        coVerify(exactly = 1) { repository.delete(10L, 1L) }
    }

    @Test
    fun insertWritesTrack() = runTest {
        coEvery { repository.insert(row) } returns Unit

        InsertTrack(repository).await(row)

        coVerify(exactly = 1) { repository.insert(row) }
    }

    @Test
    fun insertSwallowsFailure() = runTest {
        coEvery { repository.insert(row) } throws IllegalStateException("store failed")

        InsertTrack(repository).await(row)

        coVerify(exactly = 1) { repository.insert(row) }
    }

    @Test
    fun insertAllWritesTracks() = runTest {
        coEvery { repository.insertAll(listOf(row)) } returns Unit

        InsertTrack(repository).awaitAll(listOf(row))

        coVerify(exactly = 1) { repository.insertAll(listOf(row)) }
    }

    @Test
    fun insertAllSwallowsFailure() = runTest {
        coEvery { repository.insertAll(listOf(row)) } throws IllegalStateException("store failed")

        InsertTrack(repository).awaitAll(listOf(row))

        coVerify(exactly = 1) { repository.insertAll(listOf(row)) }
    }
}
