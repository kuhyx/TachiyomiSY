package eu.kanade.presentation.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ChapterNumberFormatterTest {

    @Test
    fun wholeNumbersDropTheFraction() {
        formatChapterNumber(12.0) shouldBe "12"
    }

    @Test
    fun fractionsKeepThreeDigits() {
        formatChapterNumber(10.25) shouldBe "10.25"
        formatChapterNumber(1.23456) shouldBe "1.235"
    }
}
