package tachiyomi.domain.chapter.interactor

import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.MergedMangaReference

/** The chapter-deduplication policies of a merged manga (SY), keyed by the merge's `chapterSortMode`. */
internal object MergedChapterDedupe {

    /** [chapterList] with cross-source duplicates removed as the merge's sort mode says; unchanged unless [dedupe]. */
    fun apply(mangaReferences: List<MergedMangaReference>, chapterList: List<Chapter>, dedupe: Boolean): List<Chapter> =
        if (dedupe) dedupeChapterList(mangaReferences, chapterList) else chapterList

    private fun dedupeChapterList(
        mangaReferences: List<MergedMangaReference>,
        chapterList: List<Chapter>,
    ): List<Chapter> {
        return when (mangaReferences.firstOrNull { it.mangaSourceId == MERGED_SOURCE_ID }?.chapterSortMode) {
            MergedMangaReference.CHAPTER_SORT_NO_DEDUPE, MergedMangaReference.CHAPTER_SORT_NONE -> {
                chapterList
            }
            MergedMangaReference.CHAPTER_SORT_PRIORITY -> {
                dedupeByPriority(mangaReferences, chapterList)
            }
            MergedMangaReference.CHAPTER_SORT_MOST_CHAPTERS -> {
                keepSource(chapterList, sourceWithMostChapters(chapterList))
            }
            MergedMangaReference.CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER -> {
                keepSource(chapterList, sourceWithHighestNumber(chapterList))
            }
            else -> {
                chapterList
            }
        }
    }

    private fun keepSource(chapterList: List<Chapter>, mangaId: Long?): List<Chapter> =
        mangaId?.let { id -> chapterList.filter { it.mangaId == id } } ?: chapterList

    private fun sourceWithMostChapters(chapterList: List<Chapter>): Long? =
        chapterList.groupBy { it.mangaId }.maxByOrNull { it.value.size }?.key

    private fun sourceWithHighestNumber(chapterList: List<Chapter>): Long? =
        chapterList.maxByOrNull { it.chapterNumber }?.mangaId

    private fun dedupeByPriority(
        mangaReferences: List<MergedMangaReference>,
        chapterList: List<Chapter>,
    ): List<Chapter> {
        val sortedChapterList = mutableListOf<Chapter>()
        chapterList.groupBy { it.mangaId }
            .entries
            .sortedBy { (mangaId) ->
                mangaReferences.find { it.mangaId == mangaId }?.chapterPriority ?: Int.MAX_VALUE
            }
            .forEach { (_, chapters) -> sortedChapterList.mergeSource(chapters) }

        return sortedChapterList.mapIndexed { index, chapter ->
            chapter.copy(sourceOrder = index.toLong())
        }
    }

    // Inserts one source's chapters in order, each right after the previous one; a numbered chapter
    // another source already contributed is skipped and its position becomes the insertion point.
    private fun MutableList<Chapter>.mergeSource(chapters: List<Chapter>) {
        var existingChapterIndex = -1
        chapters.forEach { chapter ->
            val duplicateIndex = if (chapter.isRecognizedNumber) indexOfDuplicate(chapter) else -1
            if (duplicateIndex == -1) {
                add(existingChapterIndex + 1, chapter)
                existingChapterIndex += 1
            } else {
                existingChapterIndex = duplicateIndex
            }
        }
    }

    // Index of a same-numbered chapter from another source, or -1.
    private fun List<Chapter>.indexOfDuplicate(chapter: Chapter): Int = indexOfFirst {
        it.isRecognizedNumber &&
            it.chapterNumber == chapter.chapterNumber &&
            // allow multiple chapters of the same number from the same source
            it.mangaId != chapter.mangaId
    }
}
