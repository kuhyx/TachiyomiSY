package tachiyomi.presentation.core.components

import androidx.compose.foundation.lazy.LazyListItemInfo
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.presentation.core.util.FAKE_VIEWPORT_PX
import tachiyomi.presentation.core.util.FakeLazyListItem
import tachiyomi.presentation.core.util.FakeLazyListLayoutInfo
import tachiyomi.presentation.core.util.uniformItems

private const val ITEM_PX = 100
private const val TOTAL = 20
private const val TOLERANCE = 0.001f

/** [ListSections] over a 400 px viewport with no padding: sections are items, fractional at the edges. */
internal class ListSectionsTest {
    private val geometry = ThumbGeometry(
        thumbTopPadding = 0f,
        thumbBottomPadding = 0f,
        trackHeightPx = FAKE_VIEWPORT_PX - 48f,
        heightPx = FAKE_VIEWPORT_PX.toFloat(),
    )

    private fun sections(items: List<LazyListItemInfo>, total: Int = TOTAL): ListSections =
        ListSections(FakeLazyListLayoutInfo(items, total), FAKE_VIEWPORT_PX, geometry)

    @Test
    fun edgesOfAnItem() {
        val item = FakeLazyListItem(index = 0, offset = 10, size = 5)
        item.top shouldBe 10
        item.bottom shouldBe 15
    }

    @Test
    fun listAtTheTop() {
        val sections = sections(uniformItems(count = 4, size = ITEM_PX))
        sections.remaining shouldBe 16f
        sections.scrollable shouldBe 16f
    }

    @Test
    fun scrolledEdgesAreFractions() {
        val sections = sections(uniformItems(count = 5, size = ITEM_PX, firstIndex = 3, firstOffset = -40))
        sections.remaining shouldBe (12.6f plusOrMinus TOLERANCE)
        sections.scrollable shouldBe (16f plusOrMinus TOLERANCE)
    }

    @Test
    fun itemAboveTheViewportIsSkipped() {
        val items = listOf(FakeLazyListItem(index = 2, offset = -150, size = ITEM_PX)) +
            uniformItems(count = 5, size = ITEM_PX, firstIndex = 3, firstOffset = -50)
        val sections = sections(items)
        // previous = 3.5, remaining = (450 - 400) / 100 + (20 - 8) = 12.5
        sections.remaining shouldBe (12.5f plusOrMinus TOLERANCE)
        sections.scrollable shouldBe (16f plusOrMinus TOLERANCE)
    }

    @Test
    fun stickyHeaderOverTopIsSkipped() {
        val items = listOf(FakeLazyListItem(index = 0, offset = 0, size = 50, key = "sticky:0")) +
            uniformItems(count = 4, size = ITEM_PX, firstIndex = 5, firstOffset = 0)
        val sections = sections(items)
        // previous = 5, remaining = 0 + (20 - 9) = 11
        sections.remaining shouldBe 11f
        sections.scrollable shouldBe 16f
    }

    @Test
    fun onlyStickyHeadersUseTheEdges() {
        val sections = sections(listOf(FakeLazyListItem(index = 0, offset = 0, size = 50, key = "sticky:0")), total = 1)
        sections.remaining shouldBe -7f
        sections.scrollable shouldBe -7f
    }

    @Test
    fun itemBelowTheViewportIsSkipped() {
        val sections = sections(uniformItems(count = 6, size = ITEM_PX))
        // the item at 500 px is ignored: remaining = (500 - 400) / 100 + (20 - 5) = 16
        sections.remaining shouldBe 16f
        sections.scrollable shouldBe 16f
    }

    @Test
    fun stickyBottomAndPlainStringKeys() {
        val items = listOf(FakeLazyListItem(index = 0, offset = 0, size = ITEM_PX, key = "row:0")) +
            uniformItems(count = 2, size = ITEM_PX, firstIndex = 1, firstOffset = ITEM_PX) +
            listOf(FakeLazyListItem(index = 3, offset = 300, size = ITEM_PX, key = "sticky:3"))
        val sections = sections(items)
        // bottom item is item 2: remaining = (300 - 400) / 100 + (20 - 3) = 16
        sections.remaining shouldBe 16f
        sections.scrollable shouldBe 16f
    }

    @Test
    fun targetForAVisibleSection() {
        val sections = sections(uniformItems(count = 5, size = ITEM_PX, firstIndex = 15, firstOffset = 0))
        sections.targetFor(thumbProportion = 0.9f, maxRemainingSections = 16f) shouldBe (18 to -360)
    }

    @Test
    fun targetForHiddenAndEndSections() {
        val sections = sections(uniformItems(count = 4, size = ITEM_PX))
        sections.targetFor(thumbProportion = 0.5f, maxRemainingSections = 16f) shouldBe (12 to -400)
        sections.targetFor(thumbProportion = 1f, maxRemainingSections = 16f) shouldBe (19 to -300)
    }
}
