package mihon.feature.migration.config

import io.kotest.matchers.shouldBe
import org.junit.Test

/** A drag reorders only between two selected sources; any other key is ignored rather than crashing. */
internal class ReorderIndicesTest {
    private val selected = listOf(10L, 20L, 30L)

    @Test
    fun selectedKeysGiveTheirPositions() {
        reorderIndices(selected, 30L, 10L) shouldBe (2 to 0)
    }

    @Test
    fun unknownSourceKeyIsIgnored() {
        reorderIndices(selected, 99L, 10L) shouldBe null
    }

    @Test
    fun unknownTargetKeyIsIgnored() {
        reorderIndices(selected, 10L, "available-20") shouldBe null
    }
}
