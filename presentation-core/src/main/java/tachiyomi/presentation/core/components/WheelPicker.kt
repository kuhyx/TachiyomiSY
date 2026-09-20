package tachiyomi.presentation.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.padding

private val DefaultPickerSize: DpSize = DpSize(128.dp, 128.dp)
private const val BACKGROUND_ALPHA = 0.2f
private val BackgroundBorderWidth: Dp = 1.dp

/**
 * A scroll wheel over [items]; tapping it opens a numeric field to type a value instead.
 *
 * `backgroundContent`: the highlight behind the selected row, or `null` for none.
 */
@Composable
public fun WheelNumberPicker(
    items: List<Number>,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    size: DpSize = DefaultPickerSize,
    onSelectionChanged: (index: Int) -> Unit = {},
    backgroundContent: (@Composable (size: DpSize) -> Unit)? = {
        WheelPickerDefaults.Background(size = it)
    },
) {
    WheelPicker(
        modifier = modifier,
        startIndex = startIndex,
        items = items,
        size = size,
        onSelectionChanged = onSelectionChanged,
        manualInputType = KeyboardType.Number,
        backgroundContent = backgroundContent,
    ) {
        WheelPickerDefaults.Item(text = "$it")
    }
}

/**
 * A scroll wheel over [items].
 *
 * `backgroundContent`: the highlight behind the selected row, or `null` for none.
 */
@Composable
public fun WheelTextPicker(
    items: List<String>,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    size: DpSize = DefaultPickerSize,
    onSelectionChanged: (index: Int) -> Unit = {},
    backgroundContent: (@Composable (size: DpSize) -> Unit)? = {
        WheelPickerDefaults.Background(size = it)
    },
) {
    WheelPicker(
        modifier = modifier,
        startIndex = startIndex,
        items = items,
        size = size,
        onSelectionChanged = onSelectionChanged,
        backgroundContent = backgroundContent,
    ) {
        WheelPickerDefaults.Item(text = it)
    }
}

/** The stock look of a wheel picker's rows. */
public object WheelPickerDefaults {
    /** The translucent, outlined band that marks the selected row of a picker [size] big. */
    @Composable
    public fun Background(size: DpSize) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .size(size.width, size.height / WHEEL_ROW_COUNT),
            shape = RoundedCornerShape(MaterialTheme.padding.medium),
            color = MaterialTheme.colorScheme.primary.copy(alpha = BACKGROUND_ALPHA),
            border = BorderStroke(BackgroundBorderWidth, MaterialTheme.colorScheme.primary),
            content = {},
        )
    }

    /** One row of the wheel: [text] in the title style, on one line. */
    @Composable
    public fun Item(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
    }
}
