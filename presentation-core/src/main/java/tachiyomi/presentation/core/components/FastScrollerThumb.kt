package tachiyomi.presentation.core.components

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxBy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.sample
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal val ThumbLength: Dp = 48.dp
internal val ThumbThickness: Dp = 12.dp
private val ThumbShape = RoundedCornerShape(ThumbThickness / 2)
internal val ThumbVisibilityDuration: Duration = 2.seconds
internal val ThumbScrollSampling: Duration = 0.1.seconds
internal val ThumbFadeOutAnimationSpec: TweenSpec<Float> = tween(
    durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
)

/** A plain mutable cell that survives recomposition without invalidating anything. */
internal class MutableData<T>(var value: T) {
    /** Stores [new] and returns what was there before. */
    fun swap(new: T): T {
        val old = value
        value = new
        return old
    }
}

/** The thumb's vertical geometry in pixels: where its track starts and how far it can travel. */
internal data class ThumbGeometry(
    val thumbTopPadding: Float,
    val thumbBottomPadding: Float,
    val trackHeightPx: Float,
    val heightPx: Float,
)

/** Measures the thumb's track for a viewport [contentHeight] pixels tall. */
@Composable
internal fun rememberThumbGeometry(
    contentHeight: Int,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    afterContentPadding: Int,
): ThumbGeometry = with(LocalDensity.current) {
    val thumbTopPadding = topContentPadding.toPx()
    val thumbBottomPadding = bottomContentPadding.toPx()
    val heightPx = contentHeight.toFloat() - thumbTopPadding - thumbBottomPadding - afterContentPadding
    ThumbGeometry(
        thumbTopPadding = thumbTopPadding,
        thumbBottomPadding = thumbBottomPadding,
        trackHeightPx = heightPx - ThumbLength.toPx(),
        heightPx = heightPx,
    )
}

/** A flow that ticks on every scroll; only the newest tick is kept while the thumb animates. */
@Composable
internal fun rememberScrolledTicks(): MutableSharedFlow<Unit> = remember {
    MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}

/** The thumb's opacity: fully shown on each tick of [scrolled] while [thumbAllowed], then faded out. */
@Composable
internal fun rememberThumbAlpha(
    scrolled: MutableSharedFlow<Unit>,
    thumbAllowed: () -> Boolean,
): Animatable<Float, AnimationVector1D> {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(scrolled, alpha) {
        scrolled
            .sample(ThumbScrollSampling)
            .mapLatest {
                if (thumbAllowed()) {
                    alpha.snapTo(1f)
                    delay(ThumbVisibilityDuration)
                }
                alpha.animateTo(0f, animationSpec = ThumbFadeOutAnimationSpec)
            }
            .launchIn(this)
    }
    return alpha
}

/**
 * The draggable thumb itself, [thumbOffsetY] pixels down its track. Dragging is wired only while
 * it is visible and the list is still, and it leaves the system gesture area only then too.
 */
@Composable
internal fun ScrollerThumb(
    thumbOffsetY: Float,
    onDrag: (delta: Float) -> Unit,
    dragInteractionSource: MutableInteractionSource,
    isThumbDragged: Boolean,
    isScrollInProgress: Boolean,
    alpha: Float,
    thumbColor: Color,
    horizontalPadding: Dp,
    endContentPadding: Dp,
) {
    val isThumbVisible = alpha > 0f
    Box(
        modifier = Modifier
            .offset { IntOffset(0, thumbOffsetY.roundToInt()) }
            .then(
                // Recompose opts
                if (isThumbVisible && !isScrollInProgress) {
                    Modifier.draggable(
                        interactionSource = dragInteractionSource,
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState(onDrag),
                    )
                } else {
                    Modifier
                },
            )
            .then(
                // Exclude thumb from gesture area only when needed
                if (isThumbVisible && !isThumbDragged && !isScrollInProgress) {
                    Modifier.systemGestureExclusion()
                } else {
                    Modifier
                },
            )
            .height(ThumbLength)
            .padding(horizontal = horizontalPadding)
            .padding(end = endContentPadding)
            .width(ThumbThickness)
            .alpha(alpha)
            .background(color = thumbColor, shape = ThumbShape),
    )
}

/**
 * Lays [content] out at full size and [scroller] (given the content's height in pixels and the
 * incoming constraints) at its end edge.
 */
@Composable
internal fun FastScrollerLayout(
    modifier: Modifier,
    content: @Composable () -> Unit,
    scroller: @Composable (contentHeight: Int, constraints: Constraints) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val contentPlaceable = subcompose("content", content).map { it.measure(constraints) }
        val contentHeight = contentPlaceable.fastMaxBy { it.height }?.height ?: 0
        val contentWidth = contentPlaceable.fastMaxBy { it.width }?.width ?: 0

        val scrollerConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val scrollerPlaceable = subcompose("scroller") { scroller(contentHeight, constraints) }
            .map { it.measure(scrollerConstraints) }
        val scrollerWidth = scrollerPlaceable.fastMaxBy { it.width }?.width ?: 0

        layout(contentWidth, contentHeight) {
            contentPlaceable.fastForEach { it.place(0, 0) }
            scrollerPlaceable.fastForEach { it.placeRelative(contentWidth - scrollerWidth, 0) }
        }
    }
}
