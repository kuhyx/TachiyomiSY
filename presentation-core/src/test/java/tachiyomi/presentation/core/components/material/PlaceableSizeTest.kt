package tachiyomi.presentation.core.components.material

import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Placeable
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** The widest / tallest placeable of a slot, or 0 when the slot composed nothing. */
internal class PlaceableSizeTest {
    @Test
    fun emptySlotsAreZero() {
        emptyList<Placeable>().maxWidth() shouldBe 0
        emptyList<Placeable>().maxHeight() shouldBe 0
    }

    @Test
    fun widestAndTallestWin() {
        val slot = listOf(FixedPlaceable(width = 10, height = 50), FixedPlaceable(width = 30, height = 20))
        slot.maxWidth() shouldBe 30
        slot.maxHeight() shouldBe 50
    }

    @Test
    fun fixedPlaceableReportsItsSize() {
        val placeable = FixedPlaceable(width = 7, height = 9)
        placeable.width shouldBe 7
        placeable.height shouldBe 9
        placeable.measuredWidth shouldBe 7
        placeable.measuredHeight shouldBe 9
        placeable[FirstBaseline] shouldBe AlignmentLine.Unspecified
        placeable.placements.shouldBeEmpty()
    }
}
