package tachiyomi.presentation.core.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.mockk.every
import io.mockk.mockk

internal const val FAKE_GRID_VIEWPORT_PX: Int = 400
internal const val FAKE_GRID_CELL_WIDTH: Int = 200

/**
 * A visible grid cell at [offset] with [size] in a 2-column layout. The lazy-grid info types
 * are sealed, so the cells are mocks rather than implementations.
 */
internal fun fakeGridItem(itemIndex: Int, itemOffset: IntOffset, itemSize: IntSize): LazyGridItemInfo = mockk {
    every { index } returns itemIndex
    every { offset } returns itemOffset
    every { size } returns itemSize
    every { key } returns itemIndex
    every { contentType } returns null
    every { span } returns 1
    every { row } returns itemIndex / 2
    every { column } returns itemIndex % 2
}

/**
 * A hand-built grid layout in a [FAKE_GRID_VIEWPORT_PX] square viewport.
 * [totalItemsCount] is mutable so a test can pull the grid empty under a live composition.
 */
internal class FakeLazyGridLayoutInfo(
    visibleItems: List<LazyGridItemInfo>,
    @Volatile var totalItemsCount: Int,
    afterPadding: Int = 0,
) {
    private val itemCount: () -> Int = { totalItemsCount }

    /** The mocked [LazyGridLayoutInfo] that reads [totalItemsCount] on every access. */
    val info: LazyGridLayoutInfo = mockk {
        every { visibleItemsInfo } returns visibleItems
        every { totalItemsCount } answers { itemCount() }
        every { afterContentPadding } returns afterPadding
        every { viewportStartOffset } returns 0
        every { viewportEndOffset } returns FAKE_GRID_VIEWPORT_PX
        every { viewportSize } returns IntSize(FAKE_GRID_VIEWPORT_PX, FAKE_GRID_VIEWPORT_PX)
        every { orientation } returns Orientation.Vertical
        every { reverseLayout } returns false
        every { beforeContentPadding } returns 0
        every { mainAxisItemSpacing } returns 0
        every { maxSpan } returns 2
    }
}

/** [rows] rows of two [FAKE_GRID_CELL_WIDTH] x [rowHeight] cells from [firstOffsetY], starting at item [firstIndex]. */
internal fun gridRows(rows: Int, rowHeight: Int, firstIndex: Int = 0, firstOffsetY: Int = 0): List<LazyGridItemInfo> =
    List(rows * 2) { position ->
        fakeGridItem(
            itemIndex = firstIndex + position,
            itemOffset = IntOffset(
                x = position % 2 * FAKE_GRID_CELL_WIDTH,
                y = firstOffsetY + position / 2 * rowHeight,
            ),
            itemSize = IntSize(FAKE_GRID_CELL_WIDTH, rowHeight),
        )
    }
