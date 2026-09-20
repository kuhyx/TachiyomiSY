package tachiyomi.presentation.core.util

/*
 * MIT License
 *
 * Copyright (c) 2022 Albert Chang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
 * Code taken from https://gist.github.com/mxalbert1996/33a360fcab2105a31e5355af98216f5a
 * with some modifications to handle contentPadding.
 *
 * Modifiers for regular scrollable list is omitted.
 */

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastSumBy
import tachiyomi.presentation.core.components.Scroller.STICKY_HEADER_KEY_PREFIX

/**
 * The thumb of a lazy list's scrollbar, estimated from the visible items: whether it is needed,
 * how long it is and where it starts, all in pixels along [orientation].
 */
internal class LazyListScrollbarMetrics(
    layoutInfo: LazyListLayoutInfo,
    orientation: Orientation,
    reverseDirection: Boolean,
) {
    private val viewportSize = if (orientation == Orientation.Horizontal) {
        layoutInfo.viewportSize.width
    } else {
        layoutInfo.viewportSize.height
    } - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding
    private val items = layoutInfo.visibleItemsInfo
    private val itemsSize = items.fastSumBy { it.size }
    private val estimatedItemSize = if (items.isEmpty()) 0f else itemsSize.toFloat() / items.size
    private val totalSize = estimatedItemSize * layoutInfo.totalItemsCount

    /** Whether the content overflows the viewport at all. */
    val showScrollbar: Boolean = items.size < layoutInfo.totalItemsCount || itemsSize > viewportSize

    /** The thumb's length. */
    val thumbSize: Float = viewportSize / totalSize * viewportSize

    /** Where the thumb starts, measured from the start of the viewport. */
    val startOffset: Float = if (items.isEmpty()) {
        0f
    } else {
        items
            .fastFirstOrNull { (it.key as? String)?.startsWith(STICKY_HEADER_KEY_PREFIX)?.not() ?: true }
            ?.run {
                val startPadding = if (reverseDirection) {
                    layoutInfo.afterContentPadding
                } else {
                    layoutInfo.beforeContentPadding
                }
                startPadding + ((estimatedItemSize * index - offset) / totalSize * viewportSize)
            }
            ?: 0f
    }
}

/** The draw call that paints [metrics]' thumb in [style] over this content. */
internal fun ContentDrawScope.onDrawScrollbar(
    orientation: Orientation,
    style: ScrollbarStyle,
    metrics: LazyListScrollbarMetrics,
    positionOffset: Float,
): DrawScope.() -> Unit {
    val thumbSize = metrics.thumbSize
    val scrollOffset = metrics.startOffset
    val along = if (orientation == Orientation.Horizontal) this.size.width else this.size.height
    val across = if (orientation == Orientation.Horizontal) this.size.height else this.size.width
    val start = if (style.reverseDirection) along - scrollOffset - thumbSize else scrollOffset
    val edge = if (style.atEnd) across - positionOffset - style.thickness else positionOffset
    val topLeft = if (orientation == Orientation.Horizontal) Offset(start, edge) else Offset(edge, start)
    val size = if (orientation == Orientation.Horizontal) {
        Size(thumbSize, style.thickness)
    } else {
        Size(style.thickness, thumbSize)
    }

    return {
        if (metrics.showScrollbar) {
            drawRect(
                color = style.color,
                topLeft = topLeft,
                size = size,
                alpha = style.alpha(),
            )
        }
    }
}
