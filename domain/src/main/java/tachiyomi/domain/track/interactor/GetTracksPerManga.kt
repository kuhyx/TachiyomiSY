package tachiyomi.domain.track.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

/** Every manga's tracks at once, for the library's tracking badges and filters. */
public class GetTracksPerManga(
    private val trackRepository: TrackRepository,
    private val isTrackUnfollowed: IsTrackUnfollowed,
) {

    /**
     * All tracks grouped by manga id, as a flow that re-emits on every change; unfollowed MDList
     * entries are left out, so a manga with only those maps to an empty list.
     */
    public fun subscribe(): Flow<Map<Long, List<Track>>> {
        return trackRepository.getTracksAsFlow().map { tracks ->
            tracks.groupBy { it.mangaId }
                // SY -->
                .mapValues { entry ->
                    entry.value.filterNot { isTrackUnfollowed.await(it) }
                }
            // SY <--
        }
    }
}
