package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.ui.reader.dbChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MissingChaptersTest {

    private fun numbered(id: Long, number: Float) = ReaderChapter(dbChapter(id = id).also { it.chapter_number = number })

    @Test
    fun gapBetweenNumbers() {
        calculateChapterGap(numbered(1L, 5f), numbered(2L, 2f)) shouldBe 2
    }

    @Test
    fun missingSideHasNoGap() {
        calculateChapterGap(null, numbered(2L, 2f)) shouldBe 0
        calculateChapterGap(numbered(1L, 5f), null) shouldBe 0
    }
}
