package tachiyomi.core.common.util.system

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.GRAY
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.NEAR_BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.WHITE

/**
 * 100x100 pages: the scan walks columns 2, 98, 1 and 99 (offset partners 1, 99, 0 and 100),
 * sampling every fourth row, so a column holds 25 samples.
 */
internal class EdgeScanTest {
    private fun scan(grid: PixelGrid): EdgeScan = EdgeScan(grid, EdgeSamples(grid)).run()

    private fun page(fill: Int = WHITE): FakePixelGrid = FakePixelGrid(100, 100, fill)

    @Test
    fun whitePageIsNotDark() {
        val scan = scan(page())
        scan.darkBackground shouldBe false
        scan.blackColor shouldBe WHITE
        scan.overallWhitePixels shouldBe 50
        scan.overallBlackPixels shouldBe 0
        scan.topIsBlackStreak shouldBe false
        scan.bottomIsBlackStreak shouldBe false
    }

    @Test
    fun blackPageSettlesOnFirstColumn() {
        val scan = scan(page(BLACK))
        scan.darkBackground shouldBe true
        scan.blackColor shouldBe BLACK
        scan.overallWhitePixels shouldBe 0
        scan.overallBlackPixels shouldBe 25
        scan.bottomIsBlackStreak shouldBe true
        scan.topIsBlackStreak shouldBe false
    }

    @Test
    fun dominantRightColumnTakesCorner() {
        val grid = page().paint(97..99, 0..<50, NEAR_BLACK).paint(97..99, 50..<100, BLACK)
        val scan = scan(grid)
        scan.darkBackground shouldBe true
        scan.blackColor shouldBe NEAR_BLACK
        scan.overallWhitePixels shouldBe 0
        scan.bottomIsBlackStreak shouldBe false
    }

    @Test
    fun dominantOffsetKeepsFallback() {
        val grid = page().paint(99..99, 0..<100, BLACK).paint(0..0, 0..<100, BLACK)
        val scan = scan(grid)
        scan.darkBackground shouldBe true
        scan.blackColor shouldBe WHITE
        scan.overallWhitePixels shouldBe 0
    }

    @Test
    fun weakBlackStreakDoesNotStop() {
        val grid = page().paint(0..2, 0..<56, BLACK)
        val scan = scan(grid)
        scan.overallBlackPixels shouldBe 14
        scan.overallWhitePixels shouldBe 36
        scan.darkBackground shouldBe false
        scan.topIsBlackStreak shouldBe false
    }

    @Test
    fun strongBlackStreakStopsTheScan() {
        val grid = page().paint(0..2, 0..<80, BLACK)
        val scan = scan(grid)
        scan.overallBlackPixels shouldBe 20
        scan.overallWhitePixels shouldBe 0
        scan.darkBackground shouldBe true
        scan.blackColor shouldBe BLACK
    }

    @Test
    fun manyWhitesWithoutStreakClear() {
        val grid = page(GRAY).paint(2..2, 0..<100, WHITE).paint(2..2, 48..48, GRAY)
        val scan = scan(grid)
        scan.overallWhitePixels shouldBe 24
        scan.darkBackground shouldBe false
    }

    @Test
    fun grayPageChangesNothing() {
        val scan = scan(page(GRAY))
        scan.darkBackground shouldBe false
        scan.overallWhitePixels shouldBe 0
        scan.overallBlackPixels shouldBe 0
        scan.blackColor shouldBe WHITE
    }

    @Test
    fun blackTopAndBottomRunsForceDark() {
        val grid = page(BLACK).paint(0..<100, 30..<70, WHITE)
        val scan = scan(grid)
        scan.topIsBlackStreak shouldBe true
        scan.bottomIsBlackStreak shouldBe true
        scan.darkBackground shouldBe true
        scan.overallWhitePixels shouldBe 20
        scan.overallBlackPixels shouldBe 30
    }

    @Test
    fun threeWhiteCornersVetoDark() {
        val grid = pageWith(BLACK, Sample.TOP_LEFT, Sample.MID_LEFT)
        EdgeScan(grid, EdgeSamples(grid)).darkBackground shouldBe false
        val twoDark = pageWith(BLACK, Sample.TOP_LEFT, Sample.TOP_RIGHT)
        EdgeScan(twoDark, EdgeSamples(twoDark)).darkBackground shouldBe true
    }
}
