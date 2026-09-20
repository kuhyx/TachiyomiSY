package tachiyomi.presentation.core.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws a vertical fast scroller to a lazy grid.
 *
 * VerticalGridFastScroller was written with a regularity assumption, so it is slightly
 * inaccurate for layouts with varying row sizes.
 *
 * `thumbColor`: the thumb's colour; the theme's primary when `Unspecified`.
 */
@Composable
public fun VerticalGridFastScroller(
    state: LazyGridState,
    columns: GridCells,
    arrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    thumbAllowed: () -> Boolean = { true },
    thumbColor: Color = Color.Unspecified,
    topContentPadding: Dp = Dp.Hairline,
    bottomContentPadding: Dp = Dp.Hairline,
    endContentPadding: Dp = Dp.Hairline,
    content: @Composable () -> Unit,
) {
    val slotSizesSums = rememberColumnWidthSums(
        columns = columns,
        horizontalArrangement = arrangement,
        contentPadding = contentPadding,
    )

    val density = LocalDensity.current
    val layoutInfo by remember(state) { derivedStateOf { state.layoutInfo } }
    FastScrollerLayout(modifier = modifier, content = content) { contentHeight, constraints ->
        val showScroller = remember(columns, layoutInfo.totalItemsCount) {
            layoutInfo.visibleItemsInfo.size < layoutInfo.totalItemsCount
        }
        if (showScroller) {
            GridScrollerThumb(
                state = state,
                columnCount = remember(columns) { density.slotSizesSums(constraints).size.coerceAtLeast(1) },
                columns = columns,
                contentHeight = contentHeight,
                thumbAllowed = thumbAllowed,
                thumbColor = thumbColor.takeOrElse { MaterialTheme.colorScheme.primary },
                topContentPadding = topContentPadding,
                bottomContentPadding = bottomContentPadding,
                endContentPadding = endContentPadding,
            )
        }
    }
}

@Composable
private fun GridScrollerThumb(
    state: LazyGridState,
    columnCount: Int,
    columns: GridCells,
    contentHeight: Int,
    thumbAllowed: () -> Boolean,
    thumbColor: Color,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    endContentPadding: Dp,
) {
    val layoutInfo by remember(state) { derivedStateOf { state.layoutInfo } }
    val firstVisibleItemScrollOffset by remember(state) { derivedStateOf { state.firstVisibleItemScrollOffset } }
    val geometry = rememberThumbGeometry(
        contentHeight = contentHeight,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        afterContentPadding = layoutInfo.afterContentPadding,
    )
    var thumbOffsetY by remember(geometry.thumbTopPadding) { mutableFloatStateOf(geometry.thumbTopPadding) }

    val dragInteractionSource = remember { MutableInteractionSource() }
    val isThumbDragged by dragInteractionSource.collectIsDraggedAsState()
    val scrolled = rememberScrolledTicks()
    val scrollRange = remember(columns) { computeGridScrollRange(state = state, columnCount = columnCount) }
    val extraScrollRange = (scrollRange.toFloat() - geometry.heightPx).coerceAtLeast(1f)

    // When thumb dragged
    LaunchedEffect(thumbOffsetY) {
        if (layoutInfo.totalItemsCount != 0 && isThumbDragged) {
            val avgSizePerRow = state.layoutInfo.averageRowSize(columnCount)
            val scrollRatio = (thumbOffsetY - geometry.thumbTopPadding) / geometry.trackHeightPx
            val scrollAmt = scrollRatio * extraScrollRange
            val rowNumber = (scrollAmt / avgSizePerRow).toInt()
            val rowOffset = scrollAmt - rowNumber * avgSizePerRow

            state.scrollToItem(index = columnCount * rowNumber, scrollOffset = rowOffset.roundToInt())
            scrolled.tryEmit(Unit)
        }
    }

    // When list scrolled
    LaunchedEffect(firstVisibleItemScrollOffset) {
        if (state.layoutInfo.totalItemsCount != 0 && !isThumbDragged) {
            val scrollOffset = computeGridScrollOffset(state = state, columnCount = columnCount)
            // LazyGridItemInfo doesn't always give the accurate height of the object, so we clamp
            // the proportion at 1 to ensure that there are no issues due to this -- ideally we
            // would correctly compute the value
            val proportion = (scrollOffset.toFloat() / extraScrollRange).coerceAtMost(1f)
            thumbOffsetY = geometry.trackHeightPx * proportion + geometry.thumbTopPadding
            scrolled.tryEmit(Unit)
        }
    }

    val alpha = rememberThumbAlpha(scrolled, thumbAllowed)
    ScrollerThumb(
        thumbOffsetY = thumbOffsetY,
        onDrag = { delta ->
            thumbOffsetY = (thumbOffsetY + delta)
                .coerceIn(geometry.thumbTopPadding, geometry.thumbTopPadding + geometry.trackHeightPx)
        },
        dragInteractionSource = dragInteractionSource,
        isThumbDragged = isThumbDragged,
        isScrollInProgress = state.isScrollInProgress,
        alpha = alpha.value,
        thumbColor = thumbColor,
        horizontalPadding = Dp.Hairline,
        endContentPadding = endContentPadding,
    )
}

/** The running sums of the grid's column widths, so a constraint width maps to a column count. */
@Composable
internal fun rememberColumnWidthSums(
    columns: GridCells,
    horizontalArrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues,
): Density.(Constraints) -> List<Int> = remember(columns, horizontalArrangement, contentPadding) {
    { constraints ->
        require(constraints.maxWidth != Constraints.Infinity) {
            "LazyVerticalGrid's width should be bound by parent"
        }
        val horizontalPadding = contentPadding.calculateStartPadding(LayoutDirection.Ltr) +
            contentPadding.calculateEndPadding(LayoutDirection.Ltr)
        val gridWidth = constraints.maxWidth - horizontalPadding.roundToPx()
        with(columns) {
            calculateCrossAxisCellSizes(gridWidth, horizontalArrangement.spacing.roundToPx())
                .toMutableList()
                .apply {
                    for (i in 1..<size) {
                        this[i] += this[i - 1]
                    }
                }
        }
    }
}

// The abs corrections below come from upstream; they are kept so the estimates stay identical.

/** The mean laid-out row height, from the visible items. */
internal fun androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo.averageRowSize(columnCount: Int): Float {
    val startChild = visibleItemsInfo.first()
    val endChild = visibleItemsInfo.last()
    val laidOutArea = endChild.offset.y + endChild.size.height - startChild.offset.y
    val laidOutRows = 1 + abs(endChild.index - startChild.index) / columnCount
    return laidOutArea.toFloat() / laidOutRows
}

/** How far the grid is scrolled, in pixels, estimated from the average row height. */
internal fun computeGridScrollOffset(state: LazyGridState, columnCount: Int): Int {
    if (state.layoutInfo.totalItemsCount == 0) return 0
    val visibleItems = state.layoutInfo.visibleItemsInfo
    val startChild = visibleItems.first()
    val endChild = visibleItems.last()
    val avgSizePerRow = state.layoutInfo.averageRowSize(columnCount)

    val rowsBefore = min(startChild.index, endChild.index).coerceAtLeast(0) / columnCount
    return (rowsBefore * avgSizePerRow - startChild.offset.y).roundToInt()
}

/** The grid's full content height, in pixels, estimated from the average row height. */
internal fun computeGridScrollRange(state: LazyGridState, columnCount: Int): Int {
    if (state.layoutInfo.totalItemsCount == 0) return 0
    val endChild = state.layoutInfo.visibleItemsInfo.last()
    val avgSizePerRow = state.layoutInfo.averageRowSize(columnCount)

    val totalRows = 1 + (state.layoutInfo.totalItemsCount - 1) / columnCount
    val endSpacing = avgSizePerRow - endChild.size.height
    return (endSpacing + avgSizePerRow * totalRows).roundToInt()
}
