package tachiyomi.presentation.core.util

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.ui.unit.IntSize

internal const val FAKE_VIEWPORT_PX: Int = 400

/** A visible list item starting [offset] pixels into the viewport and [size] pixels long. */
internal data class FakeLazyListItem(
    override val index: Int,
    override val offset: Int,
    override val size: Int,
    override val key: Any = index,
) : LazyListItemInfo

/**
 * A hand-built lazy list layout: a [FAKE_VIEWPORT_PX] square viewport by default.
 * [totalItemsCount] is mutable so a test can pull the list empty under a live composition.
 */
internal class FakeLazyListLayoutInfo(
    override val visibleItemsInfo: List<LazyListItemInfo>,
    override var totalItemsCount: Int,
    override val beforeContentPadding: Int = 0,
    override val afterContentPadding: Int = 0,
    override val viewportSize: IntSize = IntSize(FAKE_VIEWPORT_PX, FAKE_VIEWPORT_PX),
    override val orientation: Orientation = Orientation.Vertical,
) : LazyListLayoutInfo {
    override val viewportStartOffset: Int = 0
    override val viewportEndOffset: Int = FAKE_VIEWPORT_PX
}

/** [count] items of [size] pixels laid out back to back from [firstOffset], starting at [firstIndex]. */
internal fun uniformItems(count: Int, size: Int, firstIndex: Int = 0, firstOffset: Int = 0): List<LazyListItemInfo> =
    List(count) { FakeLazyListItem(index = firstIndex + it, offset = firstOffset + it * size, size = size) }
