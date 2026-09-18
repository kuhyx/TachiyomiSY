package eu.kanade.tachiyomi.util.lang

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class StringExtensionsTest {
    @Test
    fun chopKeepsShortStrings() {
        "abc".chop(3) shouldBe "abc"
        "abc".chop(5, "..") shouldBe "abc"
    }

    @Test
    fun chopTruncatesWithReplacement() {
        "abcdef".chop(4) shouldBe "abc…"
        "abcdef".chop(4, "..") shouldBe "ab.."
    }

    @Test
    fun truncateCenterKeepsShort() {
        "abc".truncateCenter(3) shouldBe "abc"
        "abc".truncateCenter(4, "-") shouldBe "abc"
    }

    @Test
    fun truncateCenterKeepsBothEnds() {
        "abcdefghij".truncateCenter(7) shouldBe "ab...ij"
        "abcdefghij".truncateCenter(7, "-") shouldBe "abc-hij"
    }

    @Test
    fun naturalCompareIgnoresCase() {
        "file2".compareNaturalIgnoreCase("FILE10") shouldBeLessThan 0
        "File10".compareNaturalIgnoreCase("file2") shouldBeGreaterThan 0
        "a".compareNaturalIgnoreCase("A") shouldBe 0
    }

    @Test
    fun byteSizeCountsUtf8Bytes() {
        "abc".byteSize() shouldBe 3
        "é".byteSize() shouldBe 2
    }

    @Test
    fun takeBytesKeepsShort() {
        "abc".takeBytes(3) shouldBe "abc"
        "abc".takeBytes(10) shouldBe "abc"
    }

    @Test
    fun takeBytesDropsPartialChars() {
        "abcdef".takeBytes(4) shouldBe "abcd"
        "aé".takeBytes(2) shouldBe "a"
    }
}
