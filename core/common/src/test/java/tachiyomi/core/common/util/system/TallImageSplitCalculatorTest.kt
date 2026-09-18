package tachiyomi.core.common.util.system

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TallImageSplitCalculatorTest {

    @Test
    fun noSplitWhenCountIsOne() {
        assertFalse(
            TallImageSplitCalculator.shouldSplit(
                imageWidth = 1024,
                imageHeight = 4385,
                optimalImageHeight = 4386,
            ),
        )
    }

    @Test
    fun splitsWhenCountAboveOne() {
        assertTrue(
            TallImageSplitCalculator.shouldSplit(
                imageWidth = 1024,
                imageHeight = 4385,
                optimalImageHeight = 4384,
            ),
        )
    }

    @Test
    fun partCountRoundsBoundary() {
        assertEquals(1, TallImageSplitCalculator.calculatePartCount(imageHeight = 4384, optimalImageHeight = 4384))
        assertEquals(2, TallImageSplitCalculator.calculatePartCount(imageHeight = 4385, optimalImageHeight = 4384))
    }
}
