package tachiyomi.domain.track.interactor

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.model.track
import tachiyomi.domain.track.repository.TrackRepository

internal class GetTracksPerMangaTest {

    private val repository: TrackRepository = mockk()
    private val isTrackUnfollowed = IsTrackUnfollowed()
    private val getTracksPerManga = GetTracksPerManga(repository, isTrackUnfollowed)

    @Test
    fun groupsAndDropsUnfollowed() = runTest {
        val kept = track(id = 1L, mangaId = 10L, trackerId = MDLIST, status = FOLLOWED)
        val unfollowed = track(id = 2L, mangaId = 10L, trackerId = MDLIST, status = UNFOLLOWED)
        val other = track(id = 3L, mangaId = 20L, trackerId = 1L, status = UNFOLLOWED)
        every { repository.getTracksAsFlow() } returns flowOf(listOf(kept, unfollowed, other))

        val perManga = getTracksPerManga.subscribe().first()

        perManga shouldBe mapOf(10L to listOf(kept), 20L to listOf(other))
    }

    @Test
    fun onlyUnfollowedMapsToEmpty() = runTest {
        val unfollowed = track(id = 2L, mangaId = 10L, trackerId = MDLIST, status = UNFOLLOWED)
        every { repository.getTracksAsFlow() } returns flowOf(listOf(unfollowed))

        getTracksPerManga.subscribe().first() shouldBe mapOf(10L to emptyList())
    }

    @Test
    fun unfollowedNeedsBothFlags() {
        isTrackUnfollowed.await(track(trackerId = MDLIST, status = UNFOLLOWED)) shouldBe true
        isTrackUnfollowed.await(track(trackerId = MDLIST, status = FOLLOWED)) shouldBe false
        isTrackUnfollowed.await(track(trackerId = 1L, status = UNFOLLOWED)) shouldBe false
    }

    private companion object {
        const val MDLIST = 60L
        const val UNFOLLOWED = 0L
        const val FOLLOWED = 1L
    }
}
