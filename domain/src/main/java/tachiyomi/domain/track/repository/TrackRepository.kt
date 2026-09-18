package tachiyomi.domain.track.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.track.model.Track

/** Reads and writes of the locally stored tracker links, one row per manga and tracker. */
public interface TrackRepository {

    /** The track with row id [id], or null. */
    public suspend fun getTrackById(id: Long): Track?

    // SY -->

    /** Every track of every manga. */
    public suspend fun getTracks(): List<Track>

    /** The tracks of every manga in [mangaIds], in no particular order. */
    public suspend fun getTracksByMangaIds(mangaIds: List<Long>): List<Track>
    // SY <--

    /** The tracks of manga [mangaId]. */
    public suspend fun getTracksByMangaId(mangaId: Long): List<Track>

    /** [getTracks] as a flow that re-emits on every change. */
    public fun getTracksAsFlow(): Flow<List<Track>>

    /** [getTracksByMangaId] as a flow that re-emits on every change. */
    public fun getTracksByMangaIdAsFlow(mangaId: Long): Flow<List<Track>>

    /** Deletes the track of manga [mangaId] on tracker [trackerId]. */
    public suspend fun delete(mangaId: Long, trackerId: Long)

    /** Writes [track], replacing an existing row of the same manga and tracker. */
    public suspend fun insert(track: Track)

    /** [insert] for every entry of [tracks] in one transaction. */
    public suspend fun insertAll(tracks: List<Track>)
}
