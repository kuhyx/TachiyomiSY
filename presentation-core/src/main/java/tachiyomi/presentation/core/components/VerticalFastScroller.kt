package tachiyomi.presentation.core.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastLastOrNull
import tachiyomi.presentation.core.components.Scroller.STICKY_HEADER_KEY_PREFIX
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val ListThumbHorizontalPadding: Dp = 8.dp
private const val LAYOUT_CHANGE_TOLERANCE = 0.1
private const val MIN_SCROLLABLE_SECTIONS = 0.5
private const val TOP_THUMB_PROPORTION = 0.001f

/**
 * Draws vertical fast scroller to a lazy list
 *
 * Set key with [STICKY_HEADER_KEY_PREFIX] prefix to any sticky header item in the list.
 *
 * `thumbColor`: the thumb's colour; the theme's primary when `Unspecified`.
 */
@Composable
public fun VerticalFastScroller(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thumbAllowed: () -> Boolean = { true },
    thumbColor: Color = Color.Unspecified,
    topContentPadding: Dp = Dp.Hairline,
    bottomContentPadding: Dp = Dp.Hairline,
    endContentPadding: Dp = Dp.Hairline,
    content: @Composable () -> Unit,
) {
    val hasItems by remember(listState) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.isNotEmpty() && listState.layoutInfo.totalItemsCount != 0
        }
    }
    FastScrollerLayout(modifier = modifier, content = content) { contentHeight, _ ->
        if (hasItems) {
            ListScrollerThumb(
                listState = listState,
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
private fun ListScrollerThumb(
    listState: LazyListState,
    contentHeight: Int,
    thumbAllowed: () -> Boolean,
    thumbColor: Color,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    endContentPadding: Dp,
) {
    val layoutInfo by remember(listState) { derivedStateOf { listState.layoutInfo } }
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

    // listState.isScrollInProgress occasionally flickers
    val scrollStateTracker = remember { MutableData(listState.isScrollInProgress) }
    val stableScrollInProgress = scrollStateTracker.swap(listState.isScrollInProgress) || listState.isScrollInProgress
    val anyScrollInProgress = stableScrollInProgress || isThumbDragged

    val sections = ListSections(layoutInfo, contentHeight, geometry)
    val estimateConfidence = remember { MutableData(sections.remaining) }
    val maxRemainingSections = rememberGrowingSectionEstimate(sections, anyScrollInProgress, estimateConfidence)

    if (maxRemainingSections < MIN_SCROLLABLE_SECTIONS) return

    // When thumb dragged
    LaunchedEffect(thumbOffsetY) {
        if (layoutInfo.totalItemsCount != 0 && isThumbDragged) {
            val thumbProportion = (thumbOffsetY - geometry.thumbTopPadding) / geometry.trackHeightPx
            if (thumbProportion <= TOP_THUMB_PROPORTION) {
                estimateConfidence.value = -1f
                listState.scrollToItem(index = 0, scrollOffset = 0)
            } else {
                val target = sections.targetFor(thumbProportion, maxRemainingSections)
                listState.scrollToItem(index = target.first, scrollOffset = target.second)
            }
            scrolled.tryEmit(Unit)
        }
    }

    // When list scrolled
    if (layoutInfo.totalItemsCount != 0 && !isThumbDragged) {
        val proportion = 1f - sections.remaining / maxRemainingSections
        thumbOffsetY = geometry.trackHeightPx * proportion + geometry.thumbTopPadding
        if (stableScrollInProgress) scrolled.tryEmit(Unit)
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
        isScrollInProgress = listState.isScrollInProgress,
        alpha = alpha.value,
        thumbColor = thumbColor,
        horizontalPadding = ListThumbHorizontalPadding,
        endContentPadding = endContentPadding,
    )
}

@Composable
private fun rememberGrowingSectionEstimate(
    sections: ListSections,
    anyScrollInProgress: Boolean,
    estimateConfidence: MutableData<Float>,
): Float {
    val layoutChangeTracker = remember { MutableData(sections.scrollable) }
    val previousScrollable = layoutChangeTracker.swap(sections.scrollable)
    val layoutChanged = !anyScrollInProgress && abs(previousScrollable - sections.scrollable) > LAYOUT_CHANGE_TOLERANCE

    if (layoutChanged) estimateConfidence.value = sections.remaining
    val maxRemainingSections = remember(estimateConfidence.value) { sections.scrollable }
    estimateConfidence.value = max(estimateConfidence.value, sections.remaining)
    return maxRemainingSections
}

/**
 * The list measured in "sections" (items, fractional at the edges): how many are scrolled past
 * the top and how many remain below the viewport, ignoring sticky headers at the edges.
 */
internal class ListSections(
    private val layoutInfo: LazyListLayoutInfo,
    contentHeight: Int,
    geometry: ThumbGeometry,
) {
    private val scrollHeightPx = contentHeight.toFloat() -
        layoutInfo.beforeContentPadding -
        layoutInfo.afterContentPadding -
        geometry.thumbBottomPadding
    private val visibleItems = layoutInfo.visibleItemsInfo
    private val topItem = visibleItems.fastFirstOrNull { it.bottom >= 0 && !it.isStickyHeader() }
        ?: visibleItems.first()
    private val bottomItem = visibleItems.fastLastOrNull { it.top <= scrollHeightPx && !it.isStickyHeader() }
        ?: visibleItems.last()

    private val previous = -1f * topItem.top / topItem.size.coerceAtLeast(1) + topItem.index
    val remaining = (bottomItem.bottom - scrollHeightPx) / bottomItem.size.coerceAtLeast(1) +
        (layoutInfo.totalItemsCount - (bottomItem.index + 1))
    val scrollable = previous + remaining

    /** The item index and offset to scroll to for a thumb [thumbProportion] of the way down. */
    fun targetFor(thumbProportion: Float, maxRemainingSections: Float): Pair<Int, Int> {
        val scrollRemainingSections = (1f - thumbProportion) * maxRemainingSections
        val currentSection = layoutInfo.totalItemsCount - scrollRemainingSections
        val scrollSectionIndex = currentSection.toInt().coerceAtMost(layoutInfo.totalItemsCount)
        val expectedScrollItem = visibleItems.find { it.index == scrollSectionIndex } ?: visibleItems.first()
        val scrollRelativeOffset = expectedScrollItem.size * (currentSection - scrollSectionIndex)
        val scrollSectionOffset = (scrollRelativeOffset - scrollHeightPx).roundToInt()
        val scrollItemIndex = scrollSectionIndex.coerceIn(0, layoutInfo.totalItemsCount - 1)
        val scrollItemOffset = scrollSectionOffset + (scrollSectionIndex - scrollItemIndex) * bottomItem.size
        return scrollItemIndex to scrollItemOffset
    }

    private fun LazyListItemInfo.isStickyHeader(): Boolean =
        (key as? String)?.startsWith(STICKY_HEADER_KEY_PREFIX) ?: false
}

/** Keys of list items the fast scroller must treat specially. */
public object Scroller {
    /** Prefix of the key of every sticky header item, so the scroller skips them at the edges. */
    public const val STICKY_HEADER_KEY_PREFIX: String = "sticky:"
}

/** The item's top edge in pixels from the viewport's start. */
internal val LazyListItemInfo.top: Int
    get() = offset

/** The item's bottom edge in pixels from the viewport's start. */
internal val LazyListItemInfo.bottom: Int
    get() = offset + size
