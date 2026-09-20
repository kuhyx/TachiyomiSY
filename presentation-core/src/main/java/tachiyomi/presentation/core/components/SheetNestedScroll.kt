package tachiyomi.presentation.core.components

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Lets a scrolling child hand the sheet its upward drags before it scrolls and its leftover
 * downward drags after, so the sheet follows the finger past the child's edges.
 */
internal fun <T> AnchoredDraggableState<T>.sheetNestedScrollConnection(
    onFling: (velocity: Float) -> Unit,
): NestedScrollConnection = SheetNestedScrollConnection(this, onFling)

private class SheetNestedScrollConnection<T>(
    private val state: AnchoredDraggableState<T>,
    private val onFling: (velocity: Float) -> Unit,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.y
        return if (delta < 0 && source == NestedScrollSource.UserInput) {
            state.dispatchRawDelta(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (source == NestedScrollSource.UserInput) {
            state.dispatchRawDelta(available.y).toOffset()
        } else {
            Offset.Zero
        }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = available.y
        return if (toFling < 0 && state.offset > state.anchors.minPosition()) {
            onFling(toFling)
            // since we go to the anchor with tween settling, consume all for the best UX
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        onFling(available.y)
        return if (state.targetValue != state.settledValue) {
            available
        } else {
            Velocity.Zero
        }
    }

    private fun Float.toOffset(): Offset = Offset(0f, this)
}
