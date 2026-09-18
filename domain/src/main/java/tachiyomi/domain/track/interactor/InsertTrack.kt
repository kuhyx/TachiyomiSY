package tachiyomi.domain.track.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

/** Stores tracker links locally, replacing an existing link of the same manga and tracker. */
public class InsertTrack(
    private val trackRepository: TrackRepository,
) {

    /** Writes [track]. Store failures are logged and swallowed. */
    public suspend fun await(track: Track) {
        try {
            trackRepository.insert(track)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
        }
    }

    /** Writes every entry of [tracks] in one transaction. Store failures are logged and swallowed. */
    public suspend fun awaitAll(tracks: List<Track>) {
        try {
            trackRepository.insertAll(tracks)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
        }
    }
}
