package tachiyomi.domain.chapter.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.chapter.model.Chapter

@Execution(ExecutionMode.CONCURRENT)
internal class MissingChaptersTest {

    @Test
    fun `count is 0 for empty list`() {
        emptyList<Double>().missingChaptersCount() shouldBe 0
    }

    @Test
    fun `count is 0 when all unknown`() {
        listOf(-1.0, -1.0, -1.0).missingChaptersCount() shouldBe 0
    }

    @Test
    fun `count with repeated numbers`() {
        listOf(1.0, 1.0, 1.1, 1.5, 1.6, 1.99).missingChaptersCount() shouldBe 0
    }

    @Test
    fun `count of missing chapters`() {
        listOf(-1.0, 1.0, 2.0, 2.2, 4.0, 6.0, 10.0, 11.0).missingChaptersCount() shouldBe 5
    }

    @Test
    fun `gap returns difference`() {
        calculateChapterGap(chapter(10.0), chapter(9.0)) shouldBe 0f
        calculateChapterGap(chapter(10.0), chapter(8.0)) shouldBe 1f
        calculateChapterGap(chapter(10.0), chapter(8.5)) shouldBe 1f
        calculateChapterGap(chapter(10.0), chapter(1.1)) shouldBe 8f

        calculateChapterGap(10.0, 9.0) shouldBe 0f
        calculateChapterGap(10.0, 8.0) shouldBe 1f
        calculateChapterGap(10.0, 8.5) shouldBe 1f
        calculateChapterGap(10.0, 1.1) shouldBe 8f
    }

    @Test
    fun `gap is 0 for invalid numbers`() {
        calculateChapterGap(chapter(-1.0), chapter(10.0)) shouldBe 0
        calculateChapterGap(chapter(99.0), chapter(-1.0)) shouldBe 0

        calculateChapterGap(-1.0, 10.0) shouldBe 0
        calculateChapterGap(99.0, -1.0) shouldBe 0
    }

    @Test
    fun `gap is 0 for null chapters`() {
        calculateChapterGap(null, chapter(10.0)) shouldBe 0
        calculateChapterGap(chapter(10.0), null) shouldBe 0
        calculateChapterGap(null, null) shouldBe 0
    }

    private fun chapter(number: Double) = Chapter.create().copy(
        chapterNumber = number,
    )
}
