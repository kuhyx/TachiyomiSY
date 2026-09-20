package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.util.drawVerticalScrollbar

/**
 * LazyColumn with scrollbar.
 *
 * `state`: the list state; a fresh remembered one when `null`.
 * `verticalArrangement`: top-aligned, or bottom-aligned when [reverseLayout], when `null`.
 */
@Composable
public fun ScrollbarLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val listState = state ?: rememberLazyListState()
    val arrangement = verticalArrangement ?: if (!reverseLayout) Arrangement.Top else Arrangement.Bottom
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    val positionOffset = remember(contentPadding) {
        with(density) { contentPadding.calculateEndPadding(direction).toPx() }
    }
    LazyColumn(
        modifier = modifier
            .drawVerticalScrollbar(
                state = listState,
                reverseScrolling = reverseLayout,
                positionOffsetPx = positionOffset,
            ),
        state = listState,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = arrangement,
        horizontalAlignment = horizontalAlignment,
        userScrollEnabled = userScrollEnabled,
        content = content,
    )
}

/**
 * LazyColumn with fast scroller.
 *
 * `state`: the list state; a fresh remembered one when `null`.
 * `verticalArrangement`: top-aligned, or bottom-aligned when [reverseLayout], when `null`.
 */
@Composable
public fun FastScrollLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val listState = state ?: rememberLazyListState()
    val arrangement = verticalArrangement ?: if (!reverseLayout) Arrangement.Top else Arrangement.Bottom
    VerticalFastScroller(
        listState = listState,
        modifier = modifier,
        topContentPadding = contentPadding.calculateTopPadding(),
        endContentPadding = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = arrangement,
            horizontalAlignment = horizontalAlignment,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )
    }
}
