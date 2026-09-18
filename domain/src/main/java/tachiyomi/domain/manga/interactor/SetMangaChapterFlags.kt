package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.sortDescending
import tachiyomi.domain.manga.model.sorting
import tachiyomi.domain.manga.repository.MangaRepository

/** Rewrites the chapter filter, sort and display bits of a manga's [Manga.chapterFlags]. */
public class SetMangaChapterFlags(
    private val mangaRepository: MangaRepository,
) {

    /** Sets [manga]'s downloaded filter to [flag]; true on success. */
    public suspend fun awaitSetDownloadedFilter(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Manga.CHAPTER_DOWNLOADED_MASK),
            ),
        )
    }

    /** Sets [manga]'s unread filter to [flag]; true on success. */
    public suspend fun awaitSetUnreadFilter(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Manga.CHAPTER_UNREAD_MASK),
            ),
        )
    }

    /** Sets [manga]'s bookmarked filter to [flag]; true on success. */
    public suspend fun awaitSetBookmarkFilter(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Manga.CHAPTER_BOOKMARKED_MASK),
            ),
        )
    }

    /** Sets [manga]'s chapter display mode to [flag]; true on success. */
    public suspend fun awaitSetDisplayMode(manga: Manga, flag: Long): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Manga.CHAPTER_DISPLAY_MASK),
            ),
        )
    }

    /**
     * Sorts [manga] by [flag] ascending, or flips the direction when it already sorts by [flag];
     * true on success.
     */
    public suspend fun awaitSetSortingModeOrFlipOrder(manga: Manga, flag: Long): Boolean {
        val newFlags = manga.chapterFlags.let {
            if (manga.sorting == flag) {
                // Just flip the order
                val orderFlag = if (manga.sortDescending()) {
                    Manga.CHAPTER_SORT_ASC
                } else {
                    Manga.CHAPTER_SORT_DESC
                }
                it.setFlag(orderFlag, Manga.CHAPTER_SORT_DIR_MASK)
            } else {
                // Set new flag with ascending order
                it
                    .setFlag(flag, Manga.CHAPTER_SORTING_MASK)
                    .setFlag(Manga.CHAPTER_SORT_ASC, Manga.CHAPTER_SORT_DIR_MASK)
            }
        }
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = newFlags,
            ),
        )
    }

    /** Replaces every chapter flag of manga [mangaId] with the given values; true on success. */
    public suspend fun awaitSetAllFlags(
        mangaId: Long,
        unreadFilter: Long,
        downloadedFilter: Long,
        bookmarkedFilter: Long,
        sortingMode: Long,
        sortingDirection: Long,
        displayMode: Long,
    ): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = mangaId,
                chapterFlags = 0L.setFlag(unreadFilter, Manga.CHAPTER_UNREAD_MASK)
                    .setFlag(downloadedFilter, Manga.CHAPTER_DOWNLOADED_MASK)
                    .setFlag(bookmarkedFilter, Manga.CHAPTER_BOOKMARKED_MASK)
                    .setFlag(sortingMode, Manga.CHAPTER_SORTING_MASK)
                    .setFlag(sortingDirection, Manga.CHAPTER_SORT_DIR_MASK)
                    .setFlag(displayMode, Manga.CHAPTER_DISPLAY_MASK),
            ),
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long = this and mask.inv() or (flag and mask)
}
