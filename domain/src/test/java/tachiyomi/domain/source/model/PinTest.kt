package tachiyomi.domain.source.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class PinTest {

    @Test
    fun pinCodes() {
        Pin.Unpinned.code shouldBe 0
        Pin.Pinned.code shouldBe 1
        Pin.Actual.code shouldBe 2
        Pin.Pinned.toString() shouldBe "Pinned"
    }

    @Test
    fun pinsDefaultIsUnpinned() {
        Pins() shouldBe Pins.unpinned
        Pins().code shouldBe 0
        Pins(3).code shouldBe 3
    }

    @Test
    fun fromUnionsBits() {
        Pins.from() shouldBe Pins(0)
        Pins.from(Pin.Pinned) shouldBe Pins(1)
        Pins.pinned shouldBe Pins(3)
    }

    @Test
    fun containsChecksEveryBit() {
        (Pin.Pinned in Pins.pinned) shouldBe true
        (Pin.Actual in Pins.pinned) shouldBe true
        (Pin.Actual in Pins.from(Pin.Pinned)) shouldBe false
        (Pin.Unpinned in Pins.unpinned) shouldBe true
    }

    @Test
    fun plusSetsBits() {
        Pins.unpinned + Pin.Pinned shouldBe Pins(1)
        Pins.from(Pin.Pinned) + Pin.Pinned shouldBe Pins(1)
        Pins.from(Pin.Pinned) + Pin.Actual shouldBe Pins.pinned
    }

    @Test
    fun minusTogglesBits() {
        Pins.pinned - Pin.Actual shouldBe Pins(1)
        Pins.unpinned - Pin.Pinned shouldBe Pins(1)
    }

    @Test
    fun pinsDataClassSurface() {
        val pins = Pins(2)

        pins.component1() shouldBe 2
        pins shouldBe pins.copy()
        pins.copy(code = 1) shouldNotBe pins
        pins.hashCode() shouldBe Pins(2).hashCode()
        pins.toString() shouldBe "Pins(code=2)"
    }
}
