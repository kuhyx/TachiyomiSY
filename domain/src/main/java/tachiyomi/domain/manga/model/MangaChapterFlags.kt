package tachiyomi.domain.manga.model

import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.core.common.preference.TriState
import java.time.Instant

/*
 * Views of [Manga.chapterFlags] and the update schedule, kept out of the data
 * class so it holds data only. Every member reads exactly like the former
 * member property.
 */

/** When the next chapter is expected, or null once the manga is completed. */
public val Manga.expectedNextUpdate: Instant?
    get() = nextUpdate
        .takeIf { status != SManga.COMPLETED.toLong() }
        ?.let { Instant.ofEpochMilli(it) }

/** The chapter sort key ([Manga.CHAPTER_SORTING_SOURCE] and siblings). */
public val Manga.sorting: Long
    get() = chapterFlags and Manga.CHAPTER_SORTING_MASK

/** The chapter label mode ([Manga.CHAPTER_DISPLAY_NAME] or [Manga.CHAPTER_DISPLAY_NUMBER]). */
public val Manga.displayMode: Long
    get() = chapterFlags and Manga.CHAPTER_DISPLAY_MASK

/** The raw read/unread filter bits. */
public val Manga.unreadFilterRaw: Long
    get() = chapterFlags and Manga.CHAPTER_UNREAD_MASK

/** The raw downloaded filter bits. */
public val Manga.downloadedFilterRaw: Long
    get() = chapterFlags and Manga.CHAPTER_DOWNLOADED_MASK

/** The raw bookmarked filter bits. */
public val Manga.bookmarkedFilterRaw: Long
    get() = chapterFlags and Manga.CHAPTER_BOOKMARKED_MASK

/** [unreadFilterRaw] as a tri-state. */
public val Manga.unreadFilter: TriState
    get() = when (unreadFilterRaw) {
        Manga.CHAPTER_SHOW_UNREAD -> TriState.ENABLED_IS
        Manga.CHAPTER_SHOW_READ -> TriState.ENABLED_NOT
        else -> TriState.DISABLED
    }

/** [bookmarkedFilterRaw] as a tri-state. */
public val Manga.bookmarkedFilter: TriState
    get() = when (bookmarkedFilterRaw) {
        Manga.CHAPTER_SHOW_BOOKMARKED -> TriState.ENABLED_IS
        Manga.CHAPTER_SHOW_NOT_BOOKMARKED -> TriState.ENABLED_NOT
        else -> TriState.DISABLED
    }

/** Whether chapters are sorted newest first. */
public fun Manga.sortDescending(): Boolean = chapterFlags and Manga.CHAPTER_SORT_DIR_MASK == Manga.CHAPTER_SORT_DESC
