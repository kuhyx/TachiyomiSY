package tachiyomi.presentation.core.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** How long past a scroll the thumb's 100 ms sampler needs to have delivered the tick. */
internal const val TICK_SETTLE_MS: Long = 150L

/** Well past the 2 s visibility window plus the 250 ms fade-out. */
internal const val THUMB_GONE_MS: Long = 2_600L

/** Animates [state] to [target] whenever it changes to a non-null index (frames drive it, so it ticks). */
@Composable
internal fun AnimateListTo(state: LazyListState, target: Int?) {
    LaunchedEffect(target) {
        if (target != null) state.animateScrollToItem(target)
    }
}

/** The grid counterpart of [AnimateListTo]. */
@Composable
internal fun AnimateGridTo(state: LazyGridState, target: Int?) {
    LaunchedEffect(target) {
        if (target != null) state.animateScrollToItem(target)
    }
}
