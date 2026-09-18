package tachiyomi.domain.chapter.service

import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.sortDescending
import tachiyomi.domain.manga.model.sorting

private typealias ChapterComparator = (Chapter, Chapter) -> Int

/**
 * The chapter comparator for [manga]'s sort key, newest first when [sortDescending].
 * Source order is stored newest first, so its descending comparator is the natural one.
 */
public fun getChapterSort(
    manga: Manga,
    sortDescending: Boolean = manga.sortDescending(),
): (
    Chapter,
    Chapter,
) -> Int {
    val descending: ChapterComparator = when (manga.sorting) {
        Manga.CHAPTER_SORTING_SOURCE -> { c1, c2 -> c1.sourceOrder.compareTo(c2.sourceOrder) }
        Manga.CHAPTER_SORTING_NUMBER -> { c1, c2 -> c2.chapterNumber.compareTo(c1.chapterNumber) }
        Manga.CHAPTER_SORTING_UPLOAD_DATE -> { c1, c2 -> c2.dateUpload.compareTo(c1.dateUpload) }
        Manga.CHAPTER_SORTING_ALPHABET -> { c1, c2 -> c2.name.compareToWithCollator(c1.name) }
        else -> error("Invalid chapter sorting method: ${manga.sorting}")
    }
    return if (sortDescending) descending else { c1, c2 -> descending(c2, c1) }
}
