package tachiyomi.domain.chapter.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** The `chapterNumber` argument and the fallbacks of [ChapterRecognition.parseChapterNumber]. */
internal class ChapterRecognitionNumberTest {

    @Test
    fun knownNumberWins() {
        ChapterRecognition.parseChapterNumber("Bleach", "Bleach 567", 12.0) shouldBe 12.0
        ChapterRecognition.parseChapterNumber("Bleach", "Bleach 567", 0.0) shouldBe 0.0
    }

    @Test
    fun notAChapterIsKept() {
        ChapterRecognition.parseChapterNumber("Bleach", "Bleach 567", -2.0) shouldBe -2.0
    }

    @Test
    fun unknownNumberIsParsed() {
        ChapterRecognition.parseChapterNumber("Bleach", "Bleach 567", -1.0) shouldBe 567.0
        ChapterRecognition.parseChapterNumber("Bleach", "Bleach 567", -3.0) shouldBe 567.0
    }

    @Test
    fun unknownNumberWithNoMatch() {
        ChapterRecognition.parseChapterNumber("random", "Foo", -1.0) shouldBe -1.0
        ChapterRecognition.parseChapterNumber("random", "Foo", -3.0) shouldBe -3.0
    }

    @Test
    fun onlyTaggedNumbersFallBack() {
        // Every number is a volume tag, so the tag-stripped search finds nothing and the first one is used.
        ChapterRecognition.parseChapterNumber("random", "Vol.1 Vol.2") shouldBe 1.0
    }
}
