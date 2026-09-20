package eu.kanade.tachiyomi.util.chapter

import eu.kanade.domain.chapter.model.applyFilters
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.manga.ChapterList
import exh.source.isEhBasedManga
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.sortDescending

/**
 * Gets next unread chapter with filters and sorting applied.
 */
internal fun List<Chapter>.getNextUnread(
    manga: Manga,
    downloadManager: DownloadManager /* SY --> */,
    mergedManga: Map<Long, Manga>, /* SY <-- */
): Chapter? {
    return applyFilters(manga, downloadManager/* SY --> */, mergedManga/* SY <-- */).let { chapters ->
        when {
            // SY -->
            manga.isEhBasedManga() && manga.sortDescending() -> chapters.firstOrNull()?.takeUnless { it.read }
            manga.isEhBasedManga() -> chapters.lastOrNull()?.takeUnless { it.read }
            // SY <--
            manga.sortDescending() -> chapters.findLast { !it.read }
            else -> chapters.find { !it.read }
        }
    }
}

/**
 * Gets next unread chapter with filters and sorting applied.
 */
internal fun List<ChapterList.Item>.getNextUnread(manga: Manga): Chapter? {
    return applyFilters(manga).let { chapters ->
        when {
            // SY -->
            manga.isEhBasedManga() && manga.sortDescending() -> chapters.firstOrNull()?.takeUnless { it.chapter.read }
            manga.isEhBasedManga() -> chapters.lastOrNull()?.takeUnless { it.chapter.read }
            // SY <--
            manga.sortDescending() -> chapters.findLast { !it.chapter.read }
            else -> chapters.find { !it.chapter.read }
        }
    }?.chapter
}
