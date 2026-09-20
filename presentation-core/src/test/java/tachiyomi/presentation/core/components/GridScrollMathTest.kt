package tachiyomi.presentation.core.components

import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

private const val ROW_PX = 100
private const val COLUMNS = 2
private const val TOTAL = 20

/** The grid estimates: everything is derived from the mean laid-out row height. */
internal class GridScrollMathTest {
    private fun stateWith(layoutInfo: LazyGridLayoutInfo): LazyGridState {
        val state = mockk<LazyGridState>()
        every { state.layoutInfo } returns layoutInfo
        return state
    }

    @Test
    fun averageRowSizeOfWholeRows() {
        val info = FakeLazyGridLayoutInfo(gridRows(rows = 3, rowHeight = ROW_PX), TOTAL).info
        info.averageRowSize(COLUMNS) shouldBe 100f
    }

    @Test
    fun averageRowSizeWithPartialRow() {
        val items = gridRows(rows = 2, rowHeight = ROW_PX) +
            gridRows(rows = 1, rowHeight = ROW_PX, firstIndex = 4, firstOffsetY = 200).take(1)
        val info = FakeLazyGridLayoutInfo(items, TOTAL).info
        // 300 px over 1 + (4 - 0) / 2 = 3 rows
        info.averageRowSize(COLUMNS) shouldBe 100f
    }

    @Test
    fun scrollOffsetCountsTheRowsAbove() {
        val rows = gridRows(rows = 3, rowHeight = ROW_PX, firstIndex = 4, firstOffsetY = -30)
        val info = FakeLazyGridLayoutInfo(rows, TOTAL).info
        computeGridScrollOffset(state = stateWith(info), columnCount = COLUMNS) shouldBe 230
    }

    @Test
    fun scrollOffsetAtTheTopIsZero() {
        val info = FakeLazyGridLayoutInfo(gridRows(rows = 3, rowHeight = ROW_PX), TOTAL).info
        computeGridScrollOffset(state = stateWith(info), columnCount = COLUMNS) shouldBe 0
    }

    @Test
    fun scrollOffsetOfEmptyGridIsZero() {
        val info = FakeLazyGridLayoutInfo(emptyList(), 0).info
        computeGridScrollOffset(state = stateWith(info), columnCount = COLUMNS) shouldBe 0
    }

    @Test
    fun scrollRangeSpansEveryRow() {
        val info = FakeLazyGridLayoutInfo(gridRows(rows = 3, rowHeight = ROW_PX), TOTAL).info
        // 1 + 19 / 2 = 10 rows of 100 px, and the last visible cell is a full row high
        computeGridScrollRange(state = stateWith(info), columnCount = COLUMNS) shouldBe 1_000
    }

    @Test
    fun scrollRangeAddsTheEndSpacing() {
        val items = gridRows(rows = 2, rowHeight = ROW_PX) +
            gridRows(rows = 1, rowHeight = 40, firstIndex = 4, firstOffsetY = 200)
        val info = FakeLazyGridLayoutInfo(items, TOTAL).info
        // rows: 240 px over 3 rows = 80 avg; end spacing 80 - 40 = 40; 10 rows -> 840
        computeGridScrollRange(state = stateWith(info), columnCount = COLUMNS) shouldBe 840
    }

    @Test
    fun scrollRangeOfAnEmptyGridIsZero() {
        val info = FakeLazyGridLayoutInfo(emptyList(), 0).info
        computeGridScrollRange(state = stateWith(info), columnCount = COLUMNS) shouldBe 0
    }
}
