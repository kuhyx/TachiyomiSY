package tachiyomi.core.common.util.system

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.WHITE

/** Branches the golden scenarios leave out; see [BackgroundChooserGoldenTest] for the pinned answers. */
internal class BackgroundChooserTest {
    private val darkTop = PageBackground.Gradient(listOf(BLACK, BLACK, WHITE, WHITE))
    private val darkBottom = PageBackground.Gradient(listOf(WHITE, WHITE, BLACK, BLACK))

    @Test
    fun eitherSmallDimensionMeansWhite() {
        val white = PageBackground.Solid(WHITE)
        BackgroundChooser.choose(FakePixelGrid(40, 100, BLACK), isLandscape = false) shouldBe white
        BackgroundChooser.choose(FakePixelGrid(100, 40, BLACK), isLandscape = false) shouldBe white
        BackgroundChooser.choose(FakePixelGrid(50, 50, BLACK), isLandscape = true) shouldBe PageBackground.Solid(BLACK)
    }

    @Test
    fun darkTopEdgeGivesDarkTop() {
        val grid = FakePixelGrid(100, 100).paint(0..<100, 0..<20, BLACK).paint(50..50, 5..5, WHITE)
        BackgroundChooser.choose(grid, isLandscape = false) shouldBe darkTop
        BackgroundChooser.choose(grid, isLandscape = true) shouldBe PageBackground.Solid(WHITE)
    }

    @Test
    fun darkBottomEdgeGivesDarkBottom() {
        val grid = FakePixelGrid(100, 100).paint(0..<100, 80..<100, BLACK).paint(50..50, 95..95, WHITE)
        BackgroundChooser.choose(grid, isLandscape = false) shouldBe darkBottom
    }

    @Test
    fun topStreakWinsOverBottomEdge() {
        val grid = FakePixelGrid(100, 100).paint(0..<100, 0..<40, BLACK)
        BackgroundChooser.choose(grid, isLandscape = false) shouldBe darkTop
    }

    @Test
    fun bottomStreakGivesDarkBottom() {
        val grid = FakePixelGrid(100, 100).paint(0..<100, 60..<100, BLACK)
        BackgroundChooser.choose(grid, isLandscape = false) shouldBe darkBottom
    }

    @Test
    fun uniformEdgeSkipsTheScan() {
        val gray = FakePixelGrid(100, 100, FakePixelGrid.GRAY)
        BackgroundChooser.choose(gray, isLandscape = false) shouldBe PageBackground.Solid(FakePixelGrid.GRAY)
        BackgroundChooser.choose(gray, isLandscape = true) shouldBe PageBackground.Solid(FakePixelGrid.GRAY)
    }
}
