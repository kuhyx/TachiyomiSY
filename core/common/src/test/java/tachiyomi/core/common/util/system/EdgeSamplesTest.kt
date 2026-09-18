package tachiyomi.core.common.util.system

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.GRAY
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.NEAR_BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.WHITE

internal class EdgeSamplesTest {
    @Test
    fun geometryFollowsTheInsets() {
        val edges = EdgeSamples(FakePixelGrid(100, 100))
        edges.left shouldBe 2
        edges.right shouldBe 98
        edges.offsetX shouldBe 1
        edges.leftOffsetX shouldBe 1
        edges.rightOffsetX shouldBe 99
        edges.sampledColumns shouldContainExactly listOf(2, 98, 1, 99)
    }

    @Test
    fun cornersAreReadAtTheInsets() {
        val grid = pageWith(BLACK, Sample.TOP_LEFT, Sample.BOTTOM_RIGHT).paint(98..98, 5..5, NEAR_BLACK)
        val edges = EdgeSamples(grid)
        edges.topLeft shouldBe BLACK
        edges.topRight shouldBe NEAR_BLACK
        edges.botLeft shouldBe WHITE
        edges.botRight shouldBe BLACK
        edges.topLeftIsDark shouldBe true
        edges.topRightIsDark shouldBe true
        edges.botLeftIsDark shouldBe false
        edges.botRightIsDark shouldBe true
        edges.topMidIsDark shouldBe false
    }

    @Test
    fun mostCornersAreWhiteNeedsThree() {
        EdgeSamples(pageWith(BLACK, Sample.TOP_LEFT)).mostCornersAreWhite shouldBe true
        EdgeSamples(pageWith(BLACK, Sample.TOP_LEFT, Sample.TOP_RIGHT)).mostCornersAreWhite shouldBe false
        EdgeSamples(FakePixelGrid(100, 100)).mostCornersAreWhite shouldBe true
    }

    @Test
    fun firstDarkCornerGoesClockwise() {
        val page = FakePixelGrid(100, 100)
            .paint(2..2, 5..5, BLACK)
            .paint(98..98, 5..5, NEAR_BLACK)
            .paint(2..2, 95..95, 0xFF050505.toInt())
            .paint(98..98, 95..95, 0xFF0A0A0A.toInt())
        EdgeSamples(page).firstDarkCorner shouldBe BLACK
        EdgeSamples(page.paint(2..2, 5..5, WHITE)).firstDarkCorner shouldBe NEAR_BLACK
        EdgeSamples(page.paint(98..98, 5..5, WHITE)).firstDarkCorner shouldBe 0xFF050505.toInt()
        EdgeSamples(page.paint(2..2, 95..95, WHITE)).firstDarkCorner shouldBe 0xFF0A0A0A.toInt()
        EdgeSamples(page.paint(98..98, 95..95, WHITE)).firstDarkCorner shouldBe WHITE
    }

    @Test
    fun cornerPairsNeedBothWhite() {
        EdgeSamples(FakePixelGrid(100, 100)).topCornersAreWhite shouldBe true
        EdgeSamples(pageWith(BLACK, Sample.TOP_RIGHT)).topCornersAreWhite shouldBe false
        EdgeSamples(pageWith(BLACK, Sample.TOP_LEFT)).topCornersAreWhite shouldBe false
        EdgeSamples(FakePixelGrid(100, 100)).botCornersAreWhite shouldBe true
        EdgeSamples(pageWith(BLACK, Sample.BOTTOM_RIGHT)).botCornersAreWhite shouldBe false
        EdgeSamples(pageWith(BLACK, Sample.BOTTOM_LEFT)).botCornersAreWhite shouldBe false
    }

    @Test
    fun uniformEdgeNeedsCloseColours() {
        EdgeSamples(FakePixelGrid(100, 100, GRAY)).uniformEdgeColor() shouldBe GRAY
        EdgeSamples(FakePixelGrid(100, 100)).uniformEdgeColor().shouldBeNull()
        val gap = FakePixelGrid(100, 100, GRAY).paint(50..50, 95..95, WHITE)
        EdgeSamples(gap).uniformEdgeColor().shouldBeNull()
        val jump = FakePixelGrid(100, 100, GRAY).paint(50..50, 5..5, BLACK)
        EdgeSamples(jump).uniformEdgeColor().shouldBeNull()
    }

    @Test
    fun rightSideDarkPrefersTop() {
        val both = pageWith(BLACK, Sample.BOTTOM_RIGHT).paint(98..98, 5..5, NEAR_BLACK)
        EdgeSamples(both).rightSideDark(GRAY) shouldBe NEAR_BLACK
        EdgeSamples(pageWith(BLACK, Sample.BOTTOM_RIGHT)).rightSideDark(GRAY) shouldBe BLACK
        EdgeSamples(pageWith(BLACK, Sample.TOP_LEFT)).rightSideDark(GRAY) shouldBe GRAY
    }
}
