package tachiyomi.core.common.util.system

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.GRAY

/** 100x100 grids: 25 rows are sampled (every fourth), the offset column is one step outward. */
internal class ColumnScanTest {
    private val offsetX = 1

    @Test
    fun whiteColumnCountsWhites() {
        val tally = ColumnScan(FakePixelGrid(100, 100), x = 2, offsetX = offsetX).scan()
        tally.whitePixels shouldBe 25
        tally.blackPixels shouldBe 0
        tally.hasWhiteStreak shouldBe true
    }

    @Test
    fun darkColumnWithOffsetIsBlack() {
        val grid = FakePixelGrid(100, 100).paint(1..2, 0..<100, BLACK)
        val tally = ColumnScan(grid, x = 2, offsetX = offsetX).scan()
        tally.blackPixels shouldBe 25
        tally.hasBlackStreak shouldBe true
        tally.bottomBlackRun shouldBe 25
    }

    @Test
    fun darkColumnWithoutOffsetIsOther() {
        val grid = FakePixelGrid(100, 100).paint(2..2, 0..<100, BLACK)
        val tally = ColumnScan(grid, x = 2, offsetX = offsetX).scan()
        tally.blackPixels shouldBe 0
        tally.whitePixels shouldBe 0
    }

    @Test
    fun rightHalfColumnsOffsetOutward() {
        val grid = FakePixelGrid(100, 100).paint(98..99, 0..<100, BLACK)
        ColumnScan(grid, x = 98, offsetX = offsetX).scan().blackPixels shouldBe 25
        val unsupported = FakePixelGrid(100, 100).paint(97..98, 0..<100, BLACK)
        ColumnScan(unsupported, x = 98, offsetX = offsetX).scan().blackPixels shouldBe 0
    }

    @Test
    fun grayIsNeitherWhiteNorBlack() {
        val grid = FakePixelGrid(100, 100, GRAY)
        val tally = ColumnScan(grid, x = 2, offsetX = offsetX).scan()
        tally.whitePixels shouldBe 0
        tally.blackPixels shouldBe 0
        tally.hasWhiteStreak shouldBe false
        tally.hasBlackStreak shouldBe false
    }

    @Test
    fun mixedColumnRecordsRuns() {
        val grid = FakePixelGrid(100, 100).paint(0..<100, 0..<40, BLACK)
        val tally = ColumnScan(grid, x = 2, offsetX = offsetX).scan()
        tally.blackPixels shouldBe 10
        tally.whitePixels shouldBe 15
        tally.topBlackRun shouldBe 10
        tally.bottomWhiteRun shouldBe 15
        tally.hasWhiteStreak shouldBe true
    }
}
