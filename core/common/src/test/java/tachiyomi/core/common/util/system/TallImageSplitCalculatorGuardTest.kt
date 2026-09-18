package tachiyomi.core.common.util.system

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** The `require` guards; the happy paths live in [TallImageSplitCalculatorTest]. */
internal class TallImageSplitCalculatorGuardTest {
    @Test
    fun partCountRejectsBadHeights() {
        shouldThrow<IllegalArgumentException> {
            TallImageSplitCalculator.calculatePartCount(imageHeight = 0, optimalImageHeight = 10)
        }.message shouldBe "imageHeight must be positive"
        shouldThrow<IllegalArgumentException> {
            TallImageSplitCalculator.calculatePartCount(imageHeight = 10, optimalImageHeight = 0)
        }.message shouldBe "optimalImageHeight must be positive"
    }

    @Test
    fun shouldSplitRejectsBadWidths() {
        shouldThrow<IllegalArgumentException> {
            TallImageSplitCalculator.shouldSplit(imageWidth = 0, imageHeight = 10, optimalImageHeight = 10)
        }.message shouldBe "imageWidth must be positive"
    }

    @Test
    fun shortImagesSkipThePartCount() {
        TallImageSplitCalculator.shouldSplit(imageWidth = 10, imageHeight = 30, optimalImageHeight = 0) shouldBe false
        TallImageSplitCalculator.shouldSplit(imageWidth = 10, imageHeight = 31, optimalImageHeight = 30) shouldBe true
    }
}
