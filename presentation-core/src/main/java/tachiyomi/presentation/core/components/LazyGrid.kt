package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LazyVerticalGrid with fast scroller.
 *
 * `state`: the grid state; a fresh remembered one when `null`.
 * `thumbColor`: the thumb's colour; the theme's primary when `Unspecified`.
 * `verticalArrangement`: top-aligned, or bottom-aligned when [reverseLayout], when `null`.
 */
@Composable
public fun FastScrollLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState? = null,
    thumbAllowed: () -> Boolean = { true },
    thumbColor: Color = Color.Unspecified,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    topContentPadding: Dp = Dp.Hairline,
    bottomContentPadding: Dp = Dp.Hairline,
    endContentPadding: Dp = Dp.Hairline,
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical? = null,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit,
) {
    val gridState = state ?: rememberLazyGridState()
    VerticalGridFastScroller(
        state = gridState,
        columns = columns,
        arrangement = horizontalArrangement,
        contentPadding = contentPadding,
        modifier = modifier,
        thumbAllowed = thumbAllowed,
        thumbColor = thumbColor,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        endContentPadding = endContentPadding,
    ) {
        LazyVerticalGrid(
            columns = columns,
            state = gridState,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement ?: if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
            horizontalArrangement = horizontalArrangement,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )
    }
}
