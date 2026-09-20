package tachiyomi.presentation.core.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.showSoftKeyboard
import kotlin.math.absoluteValue

internal const val WHEEL_ROW_COUNT: Int = 3
private const val CENTER_ROW_ALPHA = 1.2f
private const val FAR_ROW_ALPHA = 0.2f

/**
 * The wheel behind [WheelNumberPicker] and [WheelTextPicker]: a snapping [LazyColumn] of
 * [items] that fades rows away from the centre and, when [manualInputType] is set, swaps to a
 * text field on tap.
 */
@Composable
internal fun <T> WheelPicker(
    items: List<T>,
    modifier: Modifier,
    startIndex: Int,
    size: DpSize,
    onSelectionChanged: (index: Int) -> Unit,
    backgroundContent: (@Composable (size: DpSize) -> Unit)?,
    manualInputType: KeyboardType? = null,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState(startIndex)
    val currentItems = rememberUpdatedState(items)

    var internalIndex by remember { mutableIntStateOf(startIndex) }
    val internalOnSelectionChanged: (Int) -> Unit = {
        internalIndex = it
        onSelectionChanged(it)
    }

    LaunchedEffect(lazyListState, onSelectionChanged) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .map { calculateSnappedItemIndex(lazyListState) }
            .distinctUntilChanged()
            .onEach {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                internalOnSelectionChanged(it)
            }
            .launchIn(this)
    }

    Box(
        modifier = modifier
            .height(size.height)
            .width(size.width),
        contentAlignment = Alignment.Center,
    ) {
        backgroundContent?.invoke(size)

        // The keyboard type of the open manual input, null while the wheel shows.
        var manualInput by remember { mutableStateOf<KeyboardType?>(null) }
        val openInput = manualInput
        if (openInput != null) {
            ManualInput(
                initialText = items[internalIndex].toString(),
                keyboardType = openInput,
                onDone = { text ->
                    currentItems.value.indexOfFirst { it.toString() == text }.takeIf { it >= 0 }?.let { index ->
                        internalOnSelectionChanged(index)
                        lazyListState.scrollToItem(index)
                    }
                    manualInput = null
                },
            )
        } else {
            Wheel(
                items = items,
                lazyListState = lazyListState,
                size = size,
                onTap = manualInputType?.let { type -> { manualInput = type } },
                itemContent = itemContent,
            )
        }
    }
}

@Composable
private fun <T> Wheel(
    items: List<T>,
    lazyListState: LazyListState,
    size: DpSize,
    onTap: (() -> Unit)?,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    val currentItems = rememberUpdatedState(items)
    LazyColumn(
        modifier = Modifier.let { if (onTap != null) it.clickableNoIndication(onClick = onTap) else it },
        state = lazyListState,
        contentPadding = PaddingValues(vertical = size.height / WHEEL_ROW_COUNT * ((WHEEL_ROW_COUNT - 1) / 2)),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState),
    ) {
        itemsIndexed(currentItems.value) { index, item ->
            Box(
                modifier = Modifier
                    .height(size.height / WHEEL_ROW_COUNT)
                    .width(size.width)
                    .alpha(calculateAnimatedAlpha(lazyListState = lazyListState, index = index)),
                contentAlignment = Alignment.Center,
            ) {
                itemContent(item)
            }
        }
    }
}

@Composable
internal fun BoxScope.ManualInput(
    initialText: String,
    keyboardType: KeyboardType,
    onDone: suspend (text: String) -> Unit,
) {
    val value = rememberSaveable(saver = TextFieldState.Saver) {
        TextFieldState(initialText = initialText, initialSelection = TextRange(initialText.length))
    }
    val scope = rememberCoroutineScope()
    val currentOnDone = rememberUpdatedState(onDone)
    val processManualInput: () -> Unit = { scope.launch { currentOnDone.value(value.text.toString()) } }

    BasicTextField(
        modifier = Modifier
            .align(Alignment.Center)
            .showSoftKeyboard(true)
            .clearFocusOnSoftKeyboardHide(processManualInput),
        onKeyboardAction = { processManualInput() },
        state = value,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        textStyle = MaterialTheme.typography.titleMedium +
            TextStyle(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    )
}

private fun LazyListState.snapOffsetForItem(itemInfo: LazyListItemInfo): Int {
    val endScrollOffset = layoutInfo.let { it.viewportEndOffset - it.afterContentPadding }
    return (endScrollOffset - itemInfo.size) / 2
}

private fun LazyListState.distanceToSnapForIndex(index: Int): Int {
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0
    return itemInfo.offset - snapOffsetForItem(itemInfo)
}

private fun calculateAnimatedAlpha(lazyListState: LazyListState, index: Int): Float {
    val distanceToIndexSnap = lazyListState.distanceToSnapForIndex(index).absoluteValue
    val viewPortHeight = lazyListState.layoutInfo.viewportSize.height.toFloat()
    val singleViewPortHeight = viewPortHeight / WHEEL_ROW_COUNT
    return if (distanceToIndexSnap <= singleViewPortHeight.toInt()) {
        CENTER_ROW_ALPHA - (distanceToIndexSnap / singleViewPortHeight)
    } else {
        FAR_ROW_ALPHA
    }
}

private fun calculateSnappedItemIndex(lazyListState: LazyListState): Int = lazyListState.layoutInfo.visibleItemsInfo
    .maxBy { calculateAnimatedAlpha(lazyListState, it.index) }
    .index
