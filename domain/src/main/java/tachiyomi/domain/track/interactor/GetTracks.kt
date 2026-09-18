package tachiyomi.domain.track.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

/** Reads of the tracker links stored locally, by row, by manga or as a whole. */
public class GetTracks(
    private val trackRepository: TrackRepository,
) {

    /** The track with row id [id], or null when there is none or the store failed (logged). */
    public suspend fun awaitOne(id: Long): Track? {
        return try {
            trackRepository.getTrackById(id)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            null
        }
    }

    // SY -->

    /** Every track of every manga; empty when the store failed (logged). */
    public suspend fun await(): List<Track> {
        return try {
            trackRepository.getTracks()
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            emptyList()
        }
    }

    /** The tracks of each manga in [mangaIds], keyed by manga id; empty when the store failed (logged). */
    public suspend fun await(mangaIds: List<Long>): Map<Long, List<Track>> {
        return try {
            trackRepository.getTracksByMangaIds(mangaIds)
                .groupBy { it.mangaId }
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            emptyMap()
        }
    }
    // SY <--

    /** The tracks of manga [mangaId]; empty when it has none or the store failed (logged). */
    public suspend fun await(mangaId: Long): List<Track> {
        return try {
            trackRepository.getTracksByMangaId(mangaId)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            emptyList()
        }
    }

    /** The tracks of manga [mangaId] as a flow that re-emits on every change. */
    public fun subscribe(mangaId: Long): Flow<List<Track>> = trackRepository.getTracksByMangaIdAsFlow(mangaId)
}
