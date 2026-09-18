package tachiyomi.data.track

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.awaitOneOrNull
import tachiyomi.data.subscribeToList
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

/** [TrackRepository] on the SQLDelight `manga_sync` table. */
public class TrackRepositoryImpl(
    private val database: Database,
) : TrackRepository {

    override suspend fun getTrackById(id: Long): Track? {
        return database.manga_syncQueries
            .getTrackById(id)
            .awaitOneOrNull(TrackMapper::mapTrack)
    }

    // SY -->
    override suspend fun getTracks(): List<Track> {
        return database.manga_syncQueries
            .getTracks()
            .awaitList(TrackMapper::mapTrack)
    }

    override suspend fun getTracksByMangaIds(mangaIds: List<Long>): List<Track> {
        return database.manga_syncQueries
            .getTracksByMangaIds(mangaIds)
            .awaitList(TrackMapper::mapTrack)
    }
    // SY <--

    override suspend fun getTracksByMangaId(mangaId: Long): List<Track> {
        return database.manga_syncQueries
            .getTracksByMangaId(mangaId)
            .awaitList(TrackMapper::mapTrack)
    }

    override fun getTracksAsFlow(): Flow<List<Track>> {
        return database.manga_syncQueries
            .getTracks()
            .subscribeToList(TrackMapper::mapTrack)
    }

    override fun getTracksByMangaIdAsFlow(mangaId: Long): Flow<List<Track>> {
        return database.manga_syncQueries
            .getTracksByMangaId(mangaId)
            .subscribeToList(TrackMapper::mapTrack)
    }

    override suspend fun delete(mangaId: Long, trackerId: Long) {
        database.manga_syncQueries.delete(
            mangaId = mangaId,
            syncId = trackerId,
        )
    }

    override suspend fun insert(track: Track) {
        insertValues(listOf(track))
    }

    override suspend fun insertAll(tracks: List<Track>) {
        insertValues(tracks)
    }

    private suspend fun insertValues(tracks: List<Track>) {
        database.transaction {
            tracks.forEach { mangaTrack ->
                database.manga_syncQueries.insert(
                    mangaId = mangaTrack.mangaId,
                    syncId = mangaTrack.trackerId,
                    remoteId = mangaTrack.remoteId,
                    libraryId = mangaTrack.libraryId,
                    title = mangaTrack.title,
                    lastChapterRead = mangaTrack.lastChapterRead,
                    totalChapters = mangaTrack.totalChapters,
                    status = mangaTrack.status,
                    score = mangaTrack.score,
                    remoteUrl = mangaTrack.remoteUrl,
                    startDate = mangaTrack.startDate,
                    finishDate = mangaTrack.finishDate,
                    private = mangaTrack.private,
                )
            }
        }
    }
}
