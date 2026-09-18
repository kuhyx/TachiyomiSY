package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** A flag without a mask, so the OR-ing arms of the operators run. */
internal data class PlainFlag(override val flag: Long) : Flag

/** A flag that owns bits 2-3. */
internal data class MaskedFlag(override val flag: Long) : FlagWithMask {
    override val mask: Long = 0b1100L
}

internal class FlagTest {

    @Test
    fun containsPlainFlagExactly() {
        val word = 0b0101L
        (PlainFlag(0b0101L) in word) shouldBe true
        (PlainFlag(0b0001L) in word) shouldBe false
    }

    @Test
    fun containsMaskedFlagUnderMask() {
        val word = 0b0111L
        (MaskedFlag(0b0100L) in word) shouldBe true
        (MaskedFlag(0b1000L) in word) shouldBe false
    }

    @Test
    fun longPlusPlainFlagOrsBits() {
        val added = 0b0001L + PlainFlag(0b0100L)
        val unchanged = 0b0101L + PlainFlag(0b0100L)
        added shouldBe 0b0101L
        unchanged shouldBe 0b0101L
    }

    @Test
    fun longPlusMaskedReplacesBits() {
        val cleared = 0b1111L + MaskedFlag(0b0100L)
        val set = 0b0011L + MaskedFlag(0b1000L)
        cleared shouldBe 0b0111L
        set shouldBe 0b1011L
    }

    @Test
    fun flagPlusPlainFlagOrsBits() {
        val plain = PlainFlag(0b0001L) + PlainFlag(0b0010L)
        val masked = MaskedFlag(0b0100L) + PlainFlag(0b0001L)
        plain shouldBe 0b0011L
        masked shouldBe 0b0101L
    }

    @Test
    fun flagPlusMaskedReplacesBits() {
        val plain = PlainFlag(0b1111L) + MaskedFlag(0b1000L)
        val masked = MaskedFlag(0b0100L) + MaskedFlag(0b1000L)
        plain shouldBe 0b1011L
        masked shouldBe 0b1000L
    }

    @Test
    fun maskedFlagIgnoresBitsOutside() {
        // Bits outside the mask on the incoming flag are dropped.
        val fromWord = 0L + MaskedFlag(0b1111L)
        val fromFlag = PlainFlag(0L) + MaskedFlag(0b1111L)
        fromWord shouldBe 0b1100L
        fromFlag shouldBe 0b1100L
    }
}
