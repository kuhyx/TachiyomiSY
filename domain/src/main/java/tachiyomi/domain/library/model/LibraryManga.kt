package tachiyomi.domain.library.model

import tachiyomi.domain.manga.model.Manga

/**
 * A library entry: the manga plus the chapter aggregates the library screen sorts and badges by.
 *
 * @property manga The manga itself.
 * @property categories Ids of the categories it belongs to.
 * @property totalChapters Number of chapters (excluded scanlators left out).
 * @property readCount Number of chapters marked read.
 * @property bookmarkCount Number of bookmarked chapters.
 * @property latestUpload Epoch millis of the newest chapter's upload date; 0 without chapters.
 * @property chapterFetchedAt Epoch millis the newest chapter was fetched; 0 without chapters.
 * @property lastRead Epoch millis a chapter was last read; 0 if never.
 */
public data class LibraryManga(
    val manga: Manga,
    val categories: List<Long>,
    val totalChapters: Long,
    val readCount: Long,
    val bookmarkCount: Long,
    val latestUpload: Long,
    val chapterFetchedAt: Long,
    val lastRead: Long,
) {
    /** The manga id. */
    val id: Long = manga.id

    /** Chapters not yet read. */
    val unreadCount: Long
        get() = totalChapters - readCount

    /** Whether any chapter is bookmarked. */
    val hasBookmarks: Boolean
        get() = bookmarkCount > 0

    /** Whether any chapter has been read. */
    val hasStarted: Boolean = readCount > 0
}
