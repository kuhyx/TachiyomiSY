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

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.sample
import tachiyomi.presentation.core.components.Scroller.STICKY_HEADER_KEY_PREFIX
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val SCROLLBAR_ALPHA = 0.364f
internal val ScrollSampling: Duration = 0.1.seconds
internal val ScrollBarVisibilityDurationMillis: Long = ViewConfiguration.getScrollDefaultDelay().toLong()
internal val ImmediateFadeOutAnimationSpec: TweenSpec<Float> = tween(
    durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
)

/**
 * Draws horizontal scrollbar to a LazyList.
 *
 * Set key with [STICKY_HEADER_KEY_PREFIX] prefix to any sticky header item in the list.
 *
 * `positionOffsetPx`: the amount of offset the scrollbar position towards the top of the layout
 */
@Composable
public fun Modifier.drawHorizontalScrollbar(
    state: LazyListState,
    reverseScrolling: Boolean = false,
    positionOffsetPx: Float = 0f,
): Modifier = drawScrollbar(state, Orientation.Horizontal, reverseScrolling, positionOffsetPx)

/**
 * Draws vertical scrollbar to a LazyList.
 *
 * Set key with [STICKY_HEADER_KEY_PREFIX] prefix to any sticky header item in the list.
 *
 * `positionOffsetPx`: the amount of offset the scrollbar position towards the start of the layout
 */
@Composable
public fun Modifier.drawVerticalScrollbar(
    state: LazyListState,
    reverseScrolling: Boolean = false,
    positionOffsetPx: Float = 0f,
): Modifier = drawScrollbar(state, Orientation.Vertical, reverseScrolling, positionOffsetPx)

/** How the scrollbar is oriented and painted; the same for every frame of one composition. */
internal data class ScrollbarStyle(
    val reverseDirection: Boolean,
    val atEnd: Boolean,
    val thickness: Float,
    val color: Color,
    val alpha: () -> Float,
)

/**
 * The scrollbar behind [drawVerticalScrollbar] and [drawHorizontalScrollbar]: shown on every
 * scroll tick, faded out after the platform's delay, drawn over the content from the list's
 * layout info.
 */
@Composable
internal fun Modifier.drawScrollbar(
    state: LazyListState,
    orientation: Orientation,
    reverseScrolling: Boolean,
    positionOffset: Float,
): Modifier {
    val scrolled = remember {
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val nestedScrollConnection = remember(orientation, scrolled) { ScrollTicker(orientation, scrolled) }

    val alpha = remember { Animatable(0f) }
    LaunchedEffect(scrolled, alpha) {
        scrolled
            .sample(ScrollSampling)
            .mapLatest {
                alpha.snapTo(1f)
                delay(ScrollBarVisibilityDurationMillis.milliseconds)
                alpha.animateTo(0f, animationSpec = ImmediateFadeOutAnimationSpec)
            }
            .launchIn(this)
    }

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val reverseDirection = if (orientation == Orientation.Horizontal) {
        if (isLtr) reverseScrolling else !reverseScrolling
    } else {
        reverseScrolling
    }
    val atEnd = if (orientation == Orientation.Vertical) isLtr else true

    val context = LocalContext.current
    val thickness = remember { ViewConfiguration.get(context).scaledScrollBarSize.toFloat() }
    val style = ScrollbarStyle(
        reverseDirection = reverseDirection,
        atEnd = atEnd,
        thickness = thickness,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = SCROLLBAR_ALPHA),
        alpha = alpha::value,
    )

    return this
        .nestedScroll(nestedScrollConnection)
        .drawWithContent {
            val metrics = LazyListScrollbarMetrics(state.layoutInfo, orientation, style.reverseDirection)
            val drawScrollbar = onDrawScrollbar(
                orientation = orientation,
                style = style,
                metrics = metrics,
                positionOffset = positionOffset,
            )
            drawContent()
            drawScrollbar()
        }
}

/** Ticks [scrolled] whenever the list consumed scroll along [orientation]. */
internal class ScrollTicker(
    private val orientation: Orientation,
    private val scrolled: MutableSharedFlow<Unit>,
) : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val delta = if (orientation == Orientation.Horizontal) consumed.x else consumed.y
        if (delta != 0f) scrolled.tryEmit(Unit)
        return Offset.Zero
    }
}
