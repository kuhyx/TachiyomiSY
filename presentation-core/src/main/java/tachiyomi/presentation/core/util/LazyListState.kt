package tachiyomi.presentation.core.util

import androidx.compose.foundation.lazy.LazyListState

/** Whether the FAB should show its label: while scrolling up, or when the list cannot scroll at all. */
public fun LazyListState.shouldExpandFAB(): Boolean = lastScrolledBackward || !canScrollForward || !canScrollBackward
