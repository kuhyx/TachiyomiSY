package tachiyomi.domain.track.interactor

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.model.track
import tachiyomi.domain.track.repository.TrackRepository

internal class GetTracksTest {

    private val repository: TrackRepository = mockk()
    private val getTracks = GetTracks(repository)
    private val first = track(id = 1L, mangaId = 10L)
    private val second = track(id = 2L, mangaId = 20L)
    private val failure = IllegalStateException("store failed")

    @Test
    fun awaitOneReturnsRow() = runTest {
        coEvery { repository.getTrackById(1L) } returns first

        getTracks.awaitOne(1L) shouldBe first
    }

    @Test
    fun awaitOneNullOnFailure() = runTest {
        coEvery { repository.getTrackById(1L) } throws failure

        getTracks.awaitOne(1L) shouldBe null
    }

    @Test
    fun awaitAllRows() = runTest {
        coEvery { repository.getTracks() } returns listOf(first, second)

        getTracks.await() shouldContainExactly listOf(first, second)
    }

    @Test
    fun awaitAllEmptyOnFailure() = runTest {
        coEvery { repository.getTracks() } throws failure

        getTracks.await().shouldBeEmpty()
    }

    @Test
    fun awaitByMangaIdsGroups() = runTest {
        coEvery { repository.getTracksByMangaIds(listOf(10L, 20L)) } returns listOf(first, second)

        getTracks.await(listOf(10L, 20L)) shouldBe mapOf(10L to listOf(first), 20L to listOf(second))
    }

    @Test
    fun awaitByMangaIdsOnFailure() = runTest {
        coEvery { repository.getTracksByMangaIds(listOf(10L)) } throws failure

        getTracks.await(listOf(10L)).shouldBeEmpty()
    }

    @Test
    fun awaitByMangaIdRows() = runTest {
        coEvery { repository.getTracksByMangaId(10L) } returns listOf(first)

        getTracks.await(10L) shouldContainExactly listOf(first)
    }

    @Test
    fun awaitByMangaIdOnFailure() = runTest {
        coEvery { repository.getTracksByMangaId(10L) } throws failure

        getTracks.await(10L).shouldBeEmpty()
    }

    @Test
    fun subscribeByMangaId() = runTest {
        every { repository.getTracksByMangaIdAsFlow(10L) } returns flowOf(listOf(first))

        getTracks.subscribe(10L).first() shouldContainExactly listOf(first)
    }
}
