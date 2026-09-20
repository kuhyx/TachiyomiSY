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

import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Material Design layout (material.io/design/layout/understanding-layout.html).
 *
 * Scaffold implements the basic material design visual layout structure.
 *
 * This component provides API to put together several material components to construct your
 * screen, by ensuring proper layout strategy for them and collecting necessary data so these
 * components will work together correctly.
 *
 * Tachiyomi changes:
 * * Pass scroll behavior to top bar by default
 * * Remove height constraint for expanded app bar
 * * Also take account of fab height when providing inner padding
 * * Fixes for fab and snackbar horizontal placements when [contentWindowInsets] is used
 * * Handle consumed window insets
 * * Add startBar slot for Navigation Rail
 *
 * @param modifier the [Modifier] to be applied to this scaffold
 * @param topBarScrollBehavior the scroll behavior handed to [topBar]; a pinned one by default
 * @param topBar top app bar of the screen, typically a small top app bar
 * @param bottomBar bottom bar of the screen, typically a navigation bar
 * @param startBar side bar on the start of the screen, typically a navigation rail
 * @param snackbarHost component to host the snackbars that are pushed to be shown
 * @param floatingActionButton Main action button of the screen
 * @param floatingActionButtonPosition position of the FAB on the screen. See [FabPosition].
 * @param containerColor the color used for the background of this scaffold; the theme's
 * background when `Unspecified`. Use [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this scaffold. Defaults to either the
 * matching content color for [containerColor], or to the current content colour if
 * [containerColor] is not a color from the theme.
 * @param contentWindowInsets window insets to be passed to content slot via PaddingValues params.
 * Scaffold will take the insets into account from the top/bottom only if the topBar/ bottomBar
 * are not present, as the scaffold expect topBar/bottomBar to handle insets instead
 * @param content content of the screen. The lambda receives a [PaddingValues] that should be
 * applied to the content root via `Modifier.padding` and `Modifier.consumeWindowInsets` to
 * properly offset top and bottom bars. If using `Modifier.verticalScroll`, apply this modifier to
 * the child of the scroll, and not on the scroll itself.
 */
@ExperimentalMaterial3Api
@Composable
public fun Scaffold(
    modifier: Modifier = Modifier,
    topBarScrollBehavior: TopAppBarScrollBehavior? = null,
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    startBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    contentWindowInsets: WindowInsets? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = topBarScrollBehavior ?: TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val container = containerColor.takeOrElse { MaterialTheme.colorScheme.background }
    val insets = contentWindowInsets ?: ScaffoldDefaults.contentWindowInsets
    // Tachiyomi: Handle consumed window insets
    val remainingWindowInsets = remember { MutableWindowInsets() }
    androidx.compose.material3.Surface(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .onConsumedWindowInsetsChanged { remainingWindowInsets.insets = insets.exclude(it) }
            .then(modifier),
        color = container,
        contentColor = contentColor.takeOrElse { contentColorFor(container) },
    ) {
        ScaffoldLayout(
            fabPosition = floatingActionButtonPosition,
            topBar = { topBar(scrollBehavior) },
            startBar = startBar,
            bottomBar = bottomBar,
            content = content,
            snackbar = snackbarHost,
            contentWindowInsets = remainingWindowInsets,
            fab = floatingActionButton,
        )
    }
}

/**
 * The possible positions for a floating action button attached to a [Scaffold].
 */
@ExperimentalMaterial3Api
@JvmInline
public value class FabPosition internal constructor(private val value: Int) {
    override fun toString(): String = if (this == Center) "FabPosition.Center" else "FabPosition.End"

    /** The two positions. */
    public companion object {
        /** Position FAB at the bottom of the screen in the center, above the navigation bar (if it exists). */
        public val Center: FabPosition = FabPosition(0)

        /** Position FAB at the bottom of the screen at the end, above the navigation bar (if it exists). */
        public val End: FabPosition = FabPosition(1)
    }
}
