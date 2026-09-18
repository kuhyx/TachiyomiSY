package tachiyomi.core.common.util.system

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.GRAY
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.TRANSPARENT_BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.WHITE

internal class PixelGridTest {
    @Test
    fun darkNeedsLowChannelsAndAlpha() {
        BLACK.isDark() shouldBe true
        0xFF101010.toInt().isDark() shouldBe true
        0xFFFF0000.toInt().isDark() shouldBe false
        0xFF0000FF.toInt().isDark() shouldBe false
        0xFF00FF00.toInt().isDark() shouldBe false
        TRANSPARENT_BLACK.isDark() shouldBe false
    }

    @Test
    fun darkThresholdIsExclusive() {
        0xFF272727.toInt().isDark() shouldBe true
        0xFF282828.toInt().isDark() shouldBe false
        0xC8000000.toInt().isDark() shouldBe false
        0xC9000000.toInt().isDark() shouldBe true
    }

    @Test
    fun closeNeedsChannelsWithinDelta() {
        GRAY.isCloseTo(GRAY) shouldBe true
        GRAY.isCloseTo(0xFF9D8080.toInt()) shouldBe true
        GRAY.isCloseTo(0xFF9E8080.toInt()) shouldBe false
        GRAY.isCloseTo(0xFF809E80.toInt()) shouldBe false
        GRAY.isCloseTo(0xFF80809E.toInt()) shouldBe false
    }

    @Test
    fun whiteIsChannelSumAboveLimit() {
        WHITE.isWhite() shouldBe true
        0xFFF8F8F8.toInt().isWhite() shouldBe true
        0xFFF6F6F6.toInt().isWhite() shouldBe false
        GRAY.isWhite() shouldBe false
        BLACK.isWhite() shouldBe false
    }

    @Test
    fun solidIsAValueClass() {
        val solid = PageBackground.Solid(BLACK)
        solid shouldBe PageBackground.Solid(BLACK)
        solid shouldNotBe PageBackground.Solid(WHITE)
        solid.hashCode() shouldBe PageBackground.Solid(BLACK).hashCode()
        "$solid" shouldBe "Solid(color=-16777216)"
        solid.copy(color = WHITE) shouldBe PageBackground.Solid(WHITE)
        solid.component1() shouldBe BLACK
        solid.color shouldBe BLACK
    }

    @Test
    fun gradientIsAValueClass() {
        val colors = listOf(BLACK, WHITE)
        val gradient = PageBackground.Gradient(colors)
        gradient shouldBe PageBackground.Gradient(listOf(BLACK, WHITE))
        gradient shouldNotBe PageBackground.Gradient(listOf(WHITE, BLACK))
        gradient.hashCode() shouldBe PageBackground.Gradient(colors).hashCode()
        "$gradient" shouldBe "Gradient(colors=[-16777216, -1])"
        gradient.copy(colors = listOf(WHITE)) shouldBe PageBackground.Gradient(listOf(WHITE))
        gradient.component1() shouldBe colors
        gradient.colors shouldBe colors
    }

    @Test
    fun fakeGridPaintsRectsAndPoints() {
        val grid = FakePixelGrid(4, 3).paint(0..1, 0..0, BLACK).paintWhere(GRAY) { x, y -> x == 3 && y == 2 }
        grid.width shouldBe 4
        grid.height shouldBe 3
        grid[0, 0] shouldBe BLACK
        grid[1, 0] shouldBe BLACK
        grid[2, 0] shouldBe WHITE
        grid[3, 2] shouldBe GRAY
        grid[2, 2] shouldBe WHITE
    }
}
