package exh.util

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MathTest {
    @Test
    fun floorRoundsDown() {
        1.7f.floor() shouldBe 1
        (-1.2f).floor() shouldBe -2
        2.9.floor() shouldBe 2
        (-0.5).floor() shouldBe -1
    }

    @Test
    fun nullIfZeroDropsZeros() {
        0.nullIfZero().shouldBeNull()
        3.nullIfZero() shouldBe 3
        0L.nullIfZero().shouldBeNull()
        4L.nullIfZero() shouldBe 4L
    }
}
