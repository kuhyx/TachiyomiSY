package tachiyomi.presentation.core.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.Slider
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.secondaryItemAlpha

private val TitleValueSpacing: Dp = 2.dp
private const val PREVIEW_MAX = 10

/**
 * A settings row with a slider; nullable parameters take the [BaseSliderItem] defaults.
 *
 * `steps`: the number of discrete stops between the range's ends; one per value when `null`.
 * `valueString`: the text shown in the value pill; the value itself when `null`.
 * `labelStyle`: the text style of [label]; body-medium when `null`.
 * `pillColor`: the value pill's background; the theme's high surface container when `Unspecified`.
 */
@Composable
public fun SliderItem(
    value: Int,
    valueRange: IntProgression,
    label: String,
    onChange: (Int) -> Unit,
    steps: Int? = null,
    valueString: String? = null,
    labelStyle: TextStyle? = null,
    pillColor: Color = Color.Unspecified,
) {
    BaseSliderItem(
        value = value,
        valueRange = valueRange,
        steps = steps,
        title = label,
        valueString = valueString,
        onChange = onChange,
        titleStyle = labelStyle ?: MaterialTheme.typography.bodyMedium,
        pillColor = pillColor,
        modifier = Modifier.padding(
            horizontal = SettingsItemsPaddings.Horizontal,
            vertical = SettingsItemsPaddings.Vertical,
        ),
    )
}

/**
 * A titled slider with the current value in a pill; nullable parameters have theme defaults.
 *
 * `steps`: the number of discrete stops between the range's ends; one per value when `null`.
 * `valueString`: the text shown in the value pill; the value itself when `null`.
 * `titleStyle`: the text style of [title]; title-large when `null`.
 * `subtitleStyle`: the text style of [subtitle]; body-small when `null`.
 * `pillColor`: the value pill's background; the theme's high surface container when `Unspecified`.
 */
@Composable
public fun BaseSliderItem(
    value: Int,
    valueRange: IntProgression,
    title: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    steps: Int? = null,
    valueString: String? = null,
    titleStyle: TextStyle? = null,
    subtitleStyle: TextStyle? = null,
    pillColor: Color = Color.Unspecified,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(TitleValueSpacing),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = titleStyle ?: MaterialTheme.typography.titleLarge,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = subtitleStyle ?: MaterialTheme.typography.bodySmall,
                        modifier = Modifier.secondaryItemAlpha(),
                    )
                }
            }
            Pill(
                text = valueString ?: value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = pillColor.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHigh },
            )
        }
        Slider(
            value = value,
            onValueChange = {
                if (it != value) {
                    onChange(it)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            valueRange = valueRange,
            steps = steps ?: with(valueRange) { last - first - 1 },
        )
    }
}

/** [BaseSliderItem] in both colour schemes, for the IDE preview. */
@Composable
@PreviewLightDark
public fun SliderItemPreview() {
    MaterialTheme(if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        var value by remember { mutableIntStateOf(0) }
        Surface {
            BaseSliderItem(
                value = value,
                valueRange = 0..PREVIEW_MAX,
                title = "Item per row",
                valueString = if (value == 0) "Auto" else value.toString(),
                onChange = { value = it },
                modifier = Modifier.padding(
                    horizontal = SettingsItemsPaddings.Horizontal,
                    vertical = SettingsItemsPaddings.Vertical,
                ),
            )
        }
    }
}
