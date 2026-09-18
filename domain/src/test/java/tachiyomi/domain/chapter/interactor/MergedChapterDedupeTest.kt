package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.chapterOf
import tachiyomi.domain.manga.model.MergedMangaReference

internal class MergedChapterDedupeTest {

    private val a1 = chapterOf(1L, 10L, 1.0)
    private val a2 = chapterOf(2L, 10L, 2.0)
    private val b1 = chapterOf(3L, 11L, 1.0)
    private val chapters = listOf(a1, a2, b1)
    private val mostChapters = listOf(mergeReference(MergedMangaReference.CHAPTER_SORT_MOST_CHAPTERS))
    private val highestNumber = listOf(mergeReference(MergedMangaReference.CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER))

    @Test
    fun dedupeOffKeepsInput() {
        MergedChapterDedupe.apply(mostChapters, chapters, dedupe = false) shouldBe chapters
    }

    @Test
    fun noMergeReferenceKeepsAll() {
        MergedChapterDedupe.apply(emptyList(), chapters, dedupe = true) shouldBe chapters
        val plainOnly = listOf(mergedReference(mangaId = 10L, mangaSourceId = 1L))
        MergedChapterDedupe.apply(plainOnly, chapters, dedupe = true) shouldBe chapters
    }

    @Test
    fun noneAndNoDedupeKeepAll() {
        val none = listOf(mergeReference(MergedMangaReference.CHAPTER_SORT_NONE))
        val noDedupe = listOf(mergeReference(MergedMangaReference.CHAPTER_SORT_NO_DEDUPE))
        MergedChapterDedupe.apply(none, chapters, dedupe = true) shouldBe chapters
        MergedChapterDedupe.apply(noDedupe, chapters, dedupe = true) shouldBe chapters
    }

    @Test
    fun unknownModeKeepsAll() {
        MergedChapterDedupe.apply(listOf(mergeReference(42)), chapters, dedupe = true) shouldBe chapters
    }

    @Test
    fun mostChaptersKeepsBiggest() {
        MergedChapterDedupe.apply(mostChapters, chapters, dedupe = true) shouldBe listOf(a1, a2)
    }

    @Test
    fun mostChaptersRunningMax() {
        // Three sources: the running maximum is kept once (11 < 10) and beaten once (12 > 10).
        val c = listOf(chapterOf(4L, 12L, 1.0), chapterOf(5L, 12L, 2.0), chapterOf(6L, 12L, 3.0))
        MergedChapterDedupe.apply(mostChapters, chapters + c, dedupe = true) shouldBe c
    }

    @Test
    fun mostChaptersEdgeLists() {
        MergedChapterDedupe.apply(mostChapters, emptyList(), dedupe = true) shouldBe emptyList()
        MergedChapterDedupe.apply(mostChapters, listOf(a1, a2), dedupe = true) shouldBe listOf(a1, a2)
        MergedChapterDedupe.apply(mostChapters, listOf(b1), dedupe = true) shouldBe listOf(b1)
    }

    @Test
    fun highestNumberKeepsThatSource() {
        MergedChapterDedupe.apply(highestNumber, chapters, dedupe = true) shouldBe listOf(a1, a2)
        MergedChapterDedupe.apply(highestNumber, listOf(b1, a1, a2), dedupe = true) shouldBe listOf(a1, a2)
    }

    @Test
    fun highestNumberEdgeLists() {
        MergedChapterDedupe.apply(highestNumber, emptyList(), dedupe = true) shouldBe emptyList()
        MergedChapterDedupe.apply(highestNumber, listOf(b1), dedupe = true) shouldBe listOf(b1)
    }
}
