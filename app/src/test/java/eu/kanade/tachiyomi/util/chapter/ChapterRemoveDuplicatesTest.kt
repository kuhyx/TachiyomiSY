package eu.kanade.tachiyomi.util.chapter

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

internal class ChapterRemoveDuplicatesTest {

    private fun chapter(id: Long, number: Double, scanlator: String? = null): Chapter =
        Chapter.create().copy(id = id, chapterNumber = number, scanlator = scanlator)

    @Test
    fun keepsCurrentThenSameScanlator() {
        val current = chapter(id = 2, number = 1.0, scanlator = "B")
        val chapters = listOf(
            chapter(id = 1, number = 1.0, scanlator = "A"),
            current,
            chapter(id = 3, number = 2.0, scanlator = "A"),
            chapter(id = 4, number = 2.0, scanlator = "B"),
            chapter(id = 5, number = 3.0, scanlator = "C"),
            chapter(id = 6, number = 3.0, scanlator = "D"),
        )
        chapters.removeDuplicates(current).map { it.id } shouldBe listOf(2L, 4L, 5L)
    }
}
