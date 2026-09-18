package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.chapterOf
import tachiyomi.domain.manga.model.MergedMangaReference

internal class MergedChapterDedupePriorityTest {

    // Source 10 (priority 2): two chapters numbered 2 and one without a number.
    private val p1 = chapterOf(1L, 10L, 1.0)
    private val p2 = chapterOf(2L, 10L, 2.0)
    private val p3 = chapterOf(3L, 10L, 2.0)
    private val p4 = chapterOf(4L, 10L, -1.0)

    // Source 11 (priority 1, so it goes first).
    private val q1 = chapterOf(5L, 11L, 1.0)
    private val q2 = chapterOf(6L, 11L, 3.0)
    private val q3 = chapterOf(7L, 11L, -1.0)

    // Source 12 has no reference and sorts last.
    private val r1 = chapterOf(8L, 12L, 2.0)
    private val r2 = chapterOf(9L, 12L, 4.0)

    // The merge's own reference comes last so the lookup walks past the plain ones.
    private val references = listOf(
        mergedReference(mangaId = 10L, mangaSourceId = 1L, chapterPriority = 2),
        mergedReference(mangaId = 11L, mangaSourceId = 2L, chapterPriority = 1),
        mergeReference(MergedMangaReference.CHAPTER_SORT_PRIORITY),
    )

    private fun ordered(vararg chapters: Chapter): List<Chapter> =
        chapters.mapIndexed { index, chapter -> chapter.copy(sourceOrder = index.toLong()) }

    @Test
    fun interleavesByPriority() {
        val input = listOf(p1, p2, p3, p4, q1, q2, q3, r1, r2)

        val result = MergedChapterDedupe.apply(references, input, dedupe = true)

        // q1 absorbs p1 and p2 slots in after it; p3 (same source, same number) is kept; r1 is a
        // duplicate of p2 so r2 lands right behind it; unnumbered chapters are never merged.
        result shouldBe ordered(q1, p2, r2, p3, p4, q2, q3)
    }

    @Test
    fun singleSourceIsRenumbered() {
        MergedChapterDedupe.apply(references, listOf(p2, p3, p1), dedupe = true) shouldBe ordered(p2, p3, p1)
    }

    @Test
    fun emptyListStaysEmpty() {
        MergedChapterDedupe.apply(references, emptyList(), dedupe = true) shouldBe emptyList()
    }
}
