package exh.util

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.Test

internal class BooleanTest {
    @Test
    fun overComparesStrictly() {
        (2 over 1).shouldBeTrue()
        (1 over 1).shouldBeFalse()
        (0 over 1).shouldBeFalse()
    }

    @Test
    fun overEqComparesInclusively() {
        (2 overEq 1).shouldBeTrue()
        (1 overEq 1).shouldBeTrue()
        (0 overEq 1).shouldBeFalse()
    }

    @Test
    fun underComparesStrictly() {
        (0 under 1).shouldBeTrue()
        (1 under 1).shouldBeFalse()
        (2 under 1).shouldBeFalse()
    }

    @Test
    fun underEqComparesInclusively() {
        (0 underEq 1).shouldBeTrue()
        (1 underEq 1).shouldBeTrue()
        (2 underEq 1).shouldBeFalse()
    }
}
