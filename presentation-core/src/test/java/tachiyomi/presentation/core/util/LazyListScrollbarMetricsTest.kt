package tachiyomi.presentation.core.util

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.ui.unit.IntSize
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private const val ITEM_PX = 100
private const val TOTAL = 16
private const val TOLERANCE = 0.01f

/** [LazyListScrollbarMetrics] over a 400 px viewport: 16 items of 100 px make a 1600 px track. */
internal class LazyListScrollbarMetricsTest {
    private fun vertical(items: List<LazyListItemInfo>, total: Int = TOTAL, reverse: Boolean = false) =
        LazyListScrollbarMetrics(FakeLazyListLayoutInfo(items, total), Orientation.Vertical, reverse)

    @Test
    fun listAtTheTop() {
        val metrics = vertical(uniformItems(count = 4, size = ITEM_PX))
        metrics.showScrollbar shouldBe true
        metrics.thumbSize shouldBe 100f
        metrics.startOffset shouldBe 0f
    }

    @Test
    fun horizontalUsesTheViewportWidth() {
        val layout = FakeLazyListLayoutInfo(
            visibleItemsInfo = uniformItems(count = 4, size = ITEM_PX),
            totalItemsCount = TOTAL,
            viewportSize = IntSize(width = FAKE_VIEWPORT_PX, height = 50),
            orientation = Orientation.Horizontal,
        )
        val metrics = LazyListScrollbarMetrics(layout, Orientation.Horizontal, false)
        metrics.showScrollbar shouldBe true
        metrics.thumbSize shouldBe 100f
    }

    @Test
    fun scrolledListMovesTheThumb() {
        val metrics = vertical(uniformItems(count = 4, size = ITEM_PX, firstIndex = 2, firstOffset = -50))
        metrics.startOffset shouldBe (62.5f plusOrMinus TOLERANCE)
    }

    @Test
    fun paddingShrinksAndOffsetsThumb() {
        val layout = FakeLazyListLayoutInfo(
            visibleItemsInfo = uniformItems(count = 4, size = ITEM_PX),
            totalItemsCount = TOTAL,
            beforeContentPadding = 5,
            afterContentPadding = 10,
        )
        val forward = LazyListScrollbarMetrics(layout, Orientation.Vertical, false)
        val reverse = LazyListScrollbarMetrics(layout, Orientation.Vertical, true)
        forward.thumbSize shouldBe (385f * 385f / 1_600f plusOrMinus TOLERANCE)
        forward.startOffset shouldBe 5f
        reverse.startOffset shouldBe 10f
    }

    @Test
    fun emptyListHasNoScrollbar() {
        val metrics = vertical(emptyList(), total = 0)
        metrics.showScrollbar shouldBe false
        metrics.thumbSize.isInfinite() shouldBe true
        metrics.startOffset shouldBe 0f
    }

    @Test
    fun allVisibleButOverflowing() {
        val metrics = vertical(uniformItems(count = 5, size = ITEM_PX), total = 5)
        metrics.showScrollbar shouldBe true
    }

    @Test
    fun everythingVisibleAndFitting() {
        val metrics = vertical(uniformItems(count = 3, size = ITEM_PX), total = 3)
        metrics.showScrollbar shouldBe false
        metrics.thumbSize shouldBe (533.33f plusOrMinus TOLERANCE)
    }

    @Test
    fun stickyHeaderSkippedAtTheStart() {
        val items = listOf(
            FakeLazyListItem(index = 0, offset = 0, size = 50, key = "sticky:0"),
            FakeLazyListItem(index = 1, offset = 50, size = ITEM_PX, key = "row:1"),
            FakeLazyListItem(index = 2, offset = 150, size = ITEM_PX),
        )
        // estimated item 250 / 3; offset = (estimate - 50) / (estimate * 16) * 400 = 10
        vertical(items).startOffset shouldBe (10f plusOrMinus TOLERANCE)
    }

    @Test
    fun onlyStickyHeadersStartAtZero() {
        val metrics = vertical(listOf(FakeLazyListItem(index = 0, offset = 20, size = 50, key = "sticky:0")))
        metrics.startOffset shouldBe 0f
    }

    @Test
    fun nonStringKeyIsAPlainItem() {
        val metrics = vertical(listOf(FakeLazyListItem(index = 3, offset = -10, size = ITEM_PX)))
        metrics.startOffset shouldBe (77.5f plusOrMinus TOLERANCE)
    }
}
