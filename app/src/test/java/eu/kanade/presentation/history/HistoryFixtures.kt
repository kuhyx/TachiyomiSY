package eu.kanade.presentation.history

import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.MangaCover
import java.util.Date

/** A history row for [mangaId]; [favorite] decides whether the add-to-library button shows. */
internal fun historyRow(
    mangaId: Long = 3L,
    chapterNumber: Double = 10.5,
    favorite: Boolean = false,
    readAt: Date? = Date(0L),
): HistoryWithRelations = HistoryWithRelations(
    id = mangaId * 10,
    chapterId = mangaId * 100,
    mangaId = mangaId,
    ogTitle = "Title $mangaId",
    chapterNumber = chapterNumber,
    readAt = readAt,
    readDuration = 1L,
    coverData = MangaCover(
        mangaId = mangaId,
        sourceId = 4L,
        isMangaFavorite = favorite,
        ogUrl = null,
        lastModified = 0L,
    ),
)
