package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.Companion.ColorFilterMode
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

// Bit offsets of the ARGB channels.
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8

@Composable
internal fun ColumnScope.ColorFilterPage(screenModel: ReaderSettingsScreenModel) {
    val customBrightness by screenModel.preferences.customBrightness.collectAsState()
    CheckboxItem(
        label = stringResource(MR.strings.pref_custom_brightness),
        pref = screenModel.preferences.customBrightness,
    )

    /*
     * Sets the brightness of the screen. Range is [-75, 100].
     * From -75 to -1 a semi-transparent black view is shown at the top with the minimum brightness.
     * From 1 to 100 it sets that value as brightness.
     * 0 sets system brightness and hides the overlay.
     */
    if (customBrightness) {
        val customBrightnessValue by screenModel.preferences.customBrightnessValue.collectAsState()
        SliderItem(
            value = customBrightnessValue,
            valueRange = -75..100,
            steps = 0,
            label = stringResource(MR.strings.pref_custom_brightness),
            onChange = { screenModel.preferences.customBrightnessValue.set(it) },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }

    val colorFilter by screenModel.preferences.colorFilter.collectAsState()
    CheckboxItem(
        label = stringResource(MR.strings.pref_custom_color_filter),
        pref = screenModel.preferences.colorFilter,
    )
    if (colorFilter) {
        ColorFilterSliders(screenModel.preferences)
    }

    CheckboxItem(
        label = stringResource(MR.strings.pref_grayscale),
        pref = screenModel.preferences.grayscale,
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_inverted_colors),
        pref = screenModel.preferences.invertedColors,
    )
}

// One slider per ARGB channel writing into the packed colorFilterValue, then the blend-mode chips.
@Composable
private fun ColorFilterSliders(preferences: ReaderPreferences) {
    val colorFilterValue by preferences.colorFilterValue.collectAsState()
    val channels = listOf(
        ChannelSlider(colorFilterValue.red, MR.strings.color_filter_r_value, RED_MASK, RED_SHIFT),
        ChannelSlider(colorFilterValue.green, MR.strings.color_filter_g_value, GREEN_MASK, GREEN_SHIFT),
        ChannelSlider(colorFilterValue.blue, MR.strings.color_filter_b_value, BLUE_MASK, 0),
        ChannelSlider(colorFilterValue.alpha, MR.strings.color_filter_a_value, ALPHA_MASK, ALPHA_SHIFT),
    )
    channels.forEach { channel ->
        SliderItem(
            value = channel.value,
            valueRange = 0..255,
            steps = 0,
            label = stringResource(channel.label),
            onChange = { newValue ->
                preferences.colorFilterValue.getAndSet {
                    getColorValue(it, newValue, channel.mask, channel.shift)
                }
            },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }

    val colorFilterMode by preferences.colorFilterMode.collectAsState()
    SettingsChipRow(MR.strings.pref_color_filter_mode) {
        ColorFilterMode.mapIndexed { index, mode ->
            FilterChip(
                selected = colorFilterMode == index,
                onClick = { preferences.colorFilterMode.set(index) },
                label = { Text(stringResource(mode.first)) },
            )
        }
    }
}

private data class ChannelSlider(val value: Int, val label: StringResource, val mask: Long, val shift: Int)

private fun getColorValue(currentColor: Int, color: Int, mask: Long, bitShift: Int): Int =
    (color shl bitShift) or (currentColor and mask.inv().toInt())
private const val ALPHA_MASK: Long = 0xFF000000
private const val RED_MASK: Long = 0x00FF0000
private const val GREEN_MASK: Long = 0x0000FF00
private const val BLUE_MASK: Long = 0x000000FF
