package tachiyomi.data.chapter

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.data.chapter.ChapterSanitizer.sanitize

internal class ChapterSanitizerTest {
    @Test
    fun stripsPrefixAndSeparators() {
        "  One Piece - Chapter 1: Romance Dawn ".sanitize("One Piece") shouldBe "Chapter 1: Romance Dawn"
        "One Piece_,:Chapter 2".sanitize("One Piece") shouldBe "Chapter 2"
    }

    @Test
    fun leavesOtherNamesAlone() {
        "Chapter 3".sanitize("One Piece") shouldBe "Chapter 3"
        "\u3000Chapter 4\u00A0".sanitize("Other") shouldBe "Chapter 4"
    }

    @Test
    fun onlyTitleBecomesEmpty() {
        "One Piece - ".sanitize("One Piece") shouldBe ""
        "".sanitize("One Piece") shouldBe ""
    }
}
