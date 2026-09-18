package tachiyomi.data.history

import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.view.GetLatestHistory
import tachiyomi.data.History as HistoryRow
import tachiyomi.view.History as HistoryViewRow

/** Domain models from the generated `history` rows and the history view rows. */
public object HistoryMapper {
    /** The [History] of a `history` row. */
    public fun mapHistory(row: HistoryRow): History = History(
        id = row._id,
        chapterId = row.chapter_id,
        readAt = row.last_read,
        readDuration = row.time_read,
    )

    /** The [HistoryWithRelations] of a history view row. */
    public fun mapHistoryWithRelations(row: HistoryViewRow): HistoryWithRelations = HistoryWithRelations(
        id = row.id,
        chapterId = row.chapterId,
        mangaId = row.mangaId,
        // SY -->
        ogTitle = row.title,
        // SY <--
        chapterNumber = row.chapterNumber,
        readAt = row.readAt,
        readDuration = row.readDuration,
        coverData = MangaCover(
            mangaId = row.mangaId,
            sourceId = row.source,
            isMangaFavorite = row.favorite,
            ogUrl = row.thumbnailUrl,
            lastModified = row.cover_last_modified,
        ),
    )

    /** The [HistoryWithRelations] of the latest-history row (the same columns as the view). */
    public fun mapLatestHistory(row: GetLatestHistory): HistoryWithRelations = HistoryWithRelations(
        id = row.id,
        chapterId = row.chapterId,
        mangaId = row.mangaId,
        // SY -->
        ogTitle = row.title,
        // SY <--
        chapterNumber = row.chapterNumber,
        readAt = row.readAt,
        readDuration = row.readDuration,
        coverData = MangaCover(
            mangaId = row.mangaId,
            sourceId = row.source,
            isMangaFavorite = row.favorite,
            ogUrl = row.thumbnailUrl,
            lastModified = row.cover_last_modified,
        ),
    )
}
