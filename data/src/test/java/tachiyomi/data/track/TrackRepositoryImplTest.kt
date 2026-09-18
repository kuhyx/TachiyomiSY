package tachiyomi.data.track

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.data.seedManga
import tachiyomi.data.seedTrack
import tachiyomi.domain.track.model.Track

internal class TrackRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = TrackRepositoryImpl(database)
    private var alphaId = 0L
    private var betaId = 0L

    @BeforeEach
    fun seed() = runTest {
        // Alpha is tracked on two trackers, Beta on one, Gamma on none.
        alphaId = database.seedManga(title = "Alpha")
        betaId = database.seedManga(title = "Beta")
        database.seedManga(title = "Gamma")
        database.seedTrack(alphaId, trackerId = 1L, remoteId = 100L)
        database.seedTrack(alphaId, trackerId = 2L, remoteId = 200L)
        database.seedTrack(betaId, trackerId = 1L, remoteId = 300L)
    }

    private fun trackOf(mangaId: Long, trackerId: Long, remoteId: Long): Track = Track(
        id = 0L, mangaId = mangaId, trackerId = trackerId, remoteId = remoteId, libraryId = 7L, title = "New $remoteId",
        lastChapterRead = 1.0, totalChapters = 3L, status = 2L, score = 5.0, remoteUrl = "https://n/$remoteId",
        startDate = 1L, finishDate = 2L, private = true,
    )

    @Test
    fun trackById() = runTest {
        val first = repository.getTracks().first()
        repository.getTrackById(first.id) shouldBe first
        repository.getTrackById(999L) shouldBe null
    }

    @Test
    fun tracksListsEveryRow() = runTest {
        val tracks = repository.getTracks()
        tracks.map { it.remoteId }.sorted() shouldBe listOf(100L, 200L, 300L)
        tracks.first { it.remoteId == 100L }.title shouldBe "Tracked 100"
        repository.getTracksAsFlow().first().size shouldBe 3
    }

    @Test
    fun tracksByMangaIds() = runTest {
        repository.getTracksByMangaIds(listOf(alphaId, betaId)).size shouldBe 3
        repository.getTracksByMangaIds(listOf(betaId)).map { it.remoteId } shouldBe listOf(300L)
        repository.getTracksByMangaIds(emptyList()) shouldBe emptyList()
    }

    @Test
    fun tracksByMangaId() = runTest {
        repository.getTracksByMangaId(alphaId).map { it.remoteId }.sorted() shouldBe listOf(100L, 200L)
        repository.getTracksByMangaIdAsFlow(betaId).first().map { it.remoteId } shouldBe listOf(300L)
        repository.getTracksByMangaId(999L) shouldBe emptyList()
    }

    @Test
    fun deleteRemovesOneTracker() = runTest {
        repository.delete(alphaId, trackerId = 1L)
        repository.getTracksByMangaId(alphaId).map { it.trackerId } shouldBe listOf(2L)
    }

    @Test
    fun insertStoresEveryColumn() = runTest {
        repository.insert(trackOf(betaId, trackerId = 2L, remoteId = 400L))
        val stored = repository.getTracksByMangaId(betaId).first { it.trackerId == 2L }
        stored shouldBe trackOf(betaId, trackerId = 2L, remoteId = 400L).copy(id = stored.id)
    }

    @Test
    fun insertReplacesSameTracker() = runTest {
        repository.insert(trackOf(betaId, trackerId = 1L, remoteId = 500L))
        repository.getTracksByMangaId(betaId).map { it.remoteId } shouldBe listOf(500L)
    }

    @Test
    fun insertAllStoresEachTrack() = runTest {
        val gammaId = betaId + 1L
        val first = trackOf(gammaId, trackerId = 1L, remoteId = 600L)
        val second = trackOf(gammaId, trackerId = 2L, remoteId = 700L)
        repository.insertAll(listOf(first, second))
        repository.insertAll(emptyList())
        repository.getTracksByMangaId(gammaId).map { it.remoteId }.sorted() shouldBe listOf(600L, 700L)
    }
}
