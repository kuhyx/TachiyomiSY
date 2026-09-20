package tachiyomi.presentation.core.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val SHEET_ANIMATION_MILLIS = 350
internal val SwipeDismissThreshold: Dp = 56.dp
private const val SHOWN = 0
private const val HIDDEN = 1

internal val SheetAnimationSpec = tween<Float>(durationMillis = SHEET_ANIMATION_MILLIS)

/** The phone half of [AdaptiveSheet]: slides up from the bottom, swipes down to dismiss. */
@Composable
internal fun BottomSheet(
    enableSwipeDismiss: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val anchoredDraggableState = rememberSaveable(saver = AnchoredDraggableState.Saver()) {
        AnchoredDraggableState(initialValue = HIDDEN)
    }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = anchoredDraggableState,
        positionalThreshold = { _: Float -> with(density) { SwipeDismissThreshold.toPx() } },
        animationSpec = SheetAnimationSpec,
    )
    val internalOnDismissRequest = {
        if (anchoredDraggableState.settledValue == SHOWN) {
            scope.launch { anchoredDraggableState.animateTo(HIDDEN) }
        }
    }
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = internalOnDismissRequest,
            )
            .fillMaxSize()
            .anchorsFromHeight(anchoredDraggableState),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = SheetMaxWidth)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = {},
                )
                .nestedScrollToSheetWhen(anchoredDraggableState, enableSwipeDismiss, scope)
                .then(modifier)
                .offsetByFiniteDrag(anchoredDraggableState)
                .anchoredDraggable(
                    state = anchoredDraggableState,
                    orientation = Orientation.Vertical,
                    enabled = enableSwipeDismiss,
                    flingBehavior = flingBehavior,
                )
                .navigationBarsPadding()
                .statusBarsPadding(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = {
                BackHandler(
                    enabled = anchoredDraggableState.targetValue == SHOWN,
                    onBack = internalOnDismissRequest,
                )
                content()
            },
        )

        ShowThenDismissWhenHidden(anchoredDraggableState, onDismissRequest)
    }
}

/** Slides the sheet in, then reports the first time it settles hidden as a dismissal. */
@Composable
internal fun ShowThenDismissWhenHidden(state: AnchoredDraggableState<Int>, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(state) {
        scope.launch { state.animateTo(SHOWN) }
        snapshotFlow { state.settledValue }
            .drop(1)
            .filter { it == HIDDEN }
            .onEach { onDismissRequest() }
            .launchIn(this)
    }
}

/** Hands the sheet its content's nested scrolls while [enableSwipeDismiss], settling it after a fling. */
@Composable
internal fun Modifier.nestedScrollToSheetWhen(
    state: AnchoredDraggableState<Int>,
    enableSwipeDismiss: Boolean,
    scope: CoroutineScope,
): Modifier = if (enableSwipeDismiss) {
    nestedScroll(
        remember(state) {
            state.sheetNestedScrollConnection { scope.launch { state.settle(SheetAnimationSpec) } }
        },
    )
} else {
    this
}

/** Moves the sheet down by its drag offset, or not at all while the anchors are not laid out yet. */
internal fun Modifier.offsetByFiniteDrag(state: AnchoredDraggableState<Int>): Modifier = offset {
    IntOffset(0, state.offset.takeIf { it.isFinite() }?.roundToInt() ?: 0)
}

private fun Modifier.anchorsFromHeight(state: AnchoredDraggableState<Int>): Modifier = onSizeChanged {
    val anchors = DraggableAnchors {
        SHOWN at 0f
        HIDDEN at it.height.toFloat()
    }
    state.updateAnchors(anchors)
}
