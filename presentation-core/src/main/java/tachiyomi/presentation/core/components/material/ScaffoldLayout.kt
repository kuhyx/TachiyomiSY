/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import kotlin.math.max

/**
 * Layout for a [Scaffold]'s content.
 *
 * @param fabPosition [FabPosition] for the FAB (if present)
 * @param topBar the content to place at the top of the [Scaffold], typically a small top app bar
 * @param startBar the content to place at the start of the [Scaffold], typically a navigation rail
 * @param content the main 'body' of the [Scaffold]
 * @param snackbar the snackbar displayed on top of the [content]
 * @param fab the floating action button displayed on top of the [content], below the [snackbar]
 * and above the [bottomBar]
 * @param contentWindowInsets the insets the [content] must keep clear of
 * @param bottomBar the content to place at the bottom of the [Scaffold], on top of the
 * [content], typically a navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScaffoldLayout(
    fabPosition: FabPosition,
    topBar: @Composable () -> Unit,
    startBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
    snackbar: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    contentWindowInsets: WindowInsets,
    bottomBar: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        // Tachiyomi: Remove height constraint for expanded app bar
        val topBarConstraints = looseConstraints.copy(maxHeight = Constraints.Infinity)

        layout(layoutWidth, layoutHeight) {
            val slots = ScaffoldSlots(this@SubcomposeLayout, contentWindowInsets, layoutWidth, layoutHeight)
            val startBarPlaceables = slots.measure(ScaffoldLayoutContent.StartBar, startBar, looseConstraints)
            slots.startBarWidth = startBarPlaceables.maxWidth()
            val topBarPlaceables = slots.measure(ScaffoldLayoutContent.TopBar, topBar, topBarConstraints)
            val snackbarPlaceables = slots.measure(ScaffoldLayoutContent.Snackbar, snackbar, looseConstraints)
            val fabPlaceables = slots.measure(ScaffoldLayoutContent.Fab, fab, looseConstraints)
            val fabPlacement = slots.fabPlacement(fabPlaceables, fabPosition)
            val bottomBarPlaceables = slots.measure(ScaffoldLayoutContent.BottomBar, bottomBar, looseConstraints)
            val bottomBarHeight = bottomBarPlaceables.maxHeight().takeIf { it != 0 }
            val fabOffsetFromBottom = fabPlacement?.let { slots.fabOffsetFromBottom(it, bottomBarHeight) }
            val snackbarHeight = snackbarPlaceables.maxHeight()
            val snackbarOffsetFromBottom = if (snackbarHeight != 0) {
                snackbarHeight + (fabOffsetFromBottom ?: slots.barOrInsetBottom(bottomBarHeight))
            } else {
                0
            }
            val bodyContentPlaceables = subcompose(ScaffoldLayoutContent.MainContent) {
                content(
                    slots.innerPadding(
                        topBarHeight = topBarPlaceables.maxHeight().takeIf { topBarPlaceables.isNotEmpty() },
                        bottomBarHeight = bottomBarHeight,
                        fabOffsetFromBottom = fabOffsetFromBottom,
                    ),
                )
            }.fastMap { it.measure(looseConstraints) }

            // Placing to control drawing order to match default elevation of each placeable
            bodyContentPlaceables.fastForEach { it.place(0, 0) }
            startBarPlaceables.fastForEach { it.placeRelative(0, 0) }
            topBarPlaceables.fastForEach { it.place(0, 0) }
            snackbarPlaceables.fastForEach {
                it.place(slots.snackbarLeft(snackbarPlaceables), layoutHeight - snackbarOffsetFromBottom)
            }
            // The bottom bar is always at the bottom of the layout
            bottomBarPlaceables.fastForEach { it.place(0, layoutHeight - (bottomBarHeight ?: 0)) }
            // Explicitly not using placeRelative here as `leftOffset` already accounts for RTL
            fabPlaceables.fastForEach {
                it.place(fabPlacement?.left ?: 0, layoutHeight - (fabOffsetFromBottom ?: 0))
            }
        }
    }
}

/** The widest placeable's width, 0 when empty. */
internal fun List<Placeable>.maxWidth(): Int = fastMaxBy { it.width }?.width ?: 0

/** The tallest placeable's height, 0 when empty. */
internal fun List<Placeable>.maxHeight(): Int = fastMaxBy { it.height }?.height ?: 0

/** The measuring arithmetic of one [ScaffoldLayout] pass, in the scope's units. */
private class ScaffoldSlots(
    private val scope: SubcomposeMeasureScope,
    private val contentWindowInsets: WindowInsets,
    private val layoutWidth: Int,
    private val layoutHeight: Int,
) {
    private val leftInset = contentWindowInsets.getLeft(scope, scope.layoutDirection)
    private val rightInset = contentWindowInsets.getRight(scope, scope.layoutDirection)
    private val bottomInset = contentWindowInsets.getBottom(scope)
    private val fabSpacing = with(scope) { FabSpacing.roundToPx() }

    /** Set once the start bar is measured; the body and the FAB are laid out beside it. */
    var startBarWidth: Int = 0

    // Tachiyomi: layoutWidth after horizontal insets
    private val insetLayoutWidth: Int
        get() = layoutWidth - leftInset - rightInset - startBarWidth

    fun measure(
        slot: ScaffoldLayoutContent,
        content: @Composable () -> Unit,
        constraints: Constraints,
    ): List<Placeable> = scope.subcompose(slot, content).fastMap { it.measure(constraints) }

    // Tachiyomi: Calculate insets for snackbar placement offset
    fun snackbarLeft(snackbarPlaceables: List<Placeable>): Int =
        (insetLayoutWidth - snackbarPlaceables.maxWidth()) / 2 + leftInset

    fun fabPlacement(fabPlaceables: List<Placeable>, fabPosition: FabPosition): FabPlacement? {
        val fabWidth = fabPlaceables.maxWidth()
        val fabHeight = fabPlaceables.maxHeight()
        if (fabPlaceables.isEmpty() || fabWidth == 0 || fabHeight == 0) return null
        // FAB distance from the left of the layout, taking into account LTR / RTL
        // Tachiyomi: Calculate insets for fab placement offset
        val fabLeftOffset = when {
            fabPosition != FabPosition.End -> leftInset + ((insetLayoutWidth - fabWidth) / 2)
            scope.layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr ->
                layoutWidth - fabSpacing - fabWidth - rightInset
            else -> fabSpacing + leftInset
        }
        return FabPlacement(left = fabLeftOffset, width = fabWidth, height = fabHeight)
    }

    fun barOrInsetBottom(bottomBarHeight: Int?): Int = max(bottomBarHeight ?: 0, bottomInset)

    fun fabOffsetFromBottom(fab: FabPlacement, bottomBarHeight: Int?): Int =
        barOrInsetBottom(bottomBarHeight) + fab.height + fabSpacing

    fun innerPadding(
        topBarHeight: Int?,
        bottomBarHeight: Int?,
        fabOffsetFromBottom: Int?,
    ): PaddingValues = with(scope) {
        val insets = contentWindowInsets.asPaddingValues(scope)
        val fabOffsetDp = fabOffsetFromBottom?.toDp() ?: 0.dp
        PaddingValues(
            top = topBarHeight?.toDp() ?: insets.calculateTopPadding(),
            // Tachiyomi: Also take account of fab height when providing inner padding
            bottom = max(bottomBarHeight?.toDp() ?: insets.calculateBottomPadding(), fabOffsetDp),
            start = max(insets.calculateStartPadding(layoutDirection), startBarWidth.toDp()),
            end = insets.calculateEndPadding(layoutDirection),
        )
    }
}

/**
 * Placement information for a floating action button inside a [Scaffold].
 *
 * @property left the FAB's offset from the left edge of the bottom bar, already adjusted for RTL
 * support
 * @property width the width of the FAB
 * @property height the height of the FAB
 */
internal data class FabPlacement(
    val left: Int,
    val width: Int,
    val height: Int,
)

// FAB spacing above the bottom bar / bottom of the Scaffold
private val FabSpacing: Dp = 16.dp

private enum class ScaffoldLayoutContent { TopBar, MainContent, Snackbar, Fab, BottomBar, StartBar }
