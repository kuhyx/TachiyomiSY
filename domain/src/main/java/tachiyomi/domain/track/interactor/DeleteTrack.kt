package tachiyomi.domain.track.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.repository.TrackRepository

/** Removes a manga's link to one tracker. */
public class DeleteTrack(
    private val trackRepository: TrackRepository,
) {

    /** Deletes the track of manga [mangaId] on tracker [trackerId]. Store failures are logged and swallowed. */
    public suspend fun await(mangaId: Long, trackerId: Long) {
        try {
            trackRepository.delete(mangaId, trackerId)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
        }
    }
}
