package tachiyomi.data.track

import tachiyomi.data.Manga_sync
import tachiyomi.domain.track.model.Track

/** Domain models from the generated `manga_sync` rows. */
public object TrackMapper {
    /** The [Track] of a `manga_sync` row. */
    public fun mapTrack(row: Manga_sync): Track = Track(
        id = row._id,
        mangaId = row.manga_id,
        trackerId = row.sync_id,
        remoteId = row.remote_id,
        libraryId = row.library_id,
        title = row.title,
        lastChapterRead = row.last_chapter_read,
        totalChapters = row.total_chapters,
        status = row.status,
        score = row.score,
        remoteUrl = row.remote_url,
        startDate = row.start_date,
        finishDate = row.finish_date,
        private = row.private_,
    )
}
