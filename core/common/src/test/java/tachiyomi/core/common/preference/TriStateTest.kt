package tachiyomi.core.common.preference

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class TriStateTest {
    @Test
    fun nextCyclesThroughEveryState() {
        TriState.DISABLED.next() shouldBe TriState.ENABLED_IS
        TriState.ENABLED_IS.next() shouldBe TriState.ENABLED_NOT
        TriState.ENABLED_NOT.next() shouldBe TriState.DISABLED
    }

    @Test
    fun entriesAreOrdered() {
        TriState.entries shouldContainExactly listOf(TriState.DISABLED, TriState.ENABLED_IS, TriState.ENABLED_NOT)
        TriState.valueOf("ENABLED_IS") shouldBe TriState.ENABLED_IS
    }
}
