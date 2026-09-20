package tachiyomi.presentation.core.components.material

import androidx.annotation.IntRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt

/**
 * An integer [Slider] over [valueRange] (`0..1` when `null`); nullable parameters take the
 * Material defaults.
 *
 * `steps`: the number of discrete stops between the range's ends; one per value when `null`.
 * `thumb`: the thumb slot; Material's thumb in [colors] when `null`.
 * `track`: the track slot; Material's track in [colors] when `null`.
 */
@Composable
public fun Slider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: IntProgression? = null,
    @IntRange(from = 0) steps: Int? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors? = null,
    interactionSource: MutableInteractionSource? = null,
    thumb: (@Composable (SliderState) -> Unit)? = null,
    track: (@Composable (SliderState) -> Unit)? = null,
) {
    val range = valueRange ?: 0..1
    val stepCount = steps ?: with(range) { last - first - 1 }
    val sliderColors = colors ?: SliderDefaults.colors()
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val state = key(stepCount, range) {
        rememberSliderState(
            value = value.toFloat(),
            steps = stepCount,
            trackRange = with(range) { first.toFloat()..last.toFloat() },
        )
    }
    state.value = value.toFloat()
    Slider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        onValueChange = { onValueChange(it.roundToInt()) },
        onValueChangeFinished = onValueChangeFinished,
        colors = sliderColors,
        interactionSource = source,
        thumb = thumb ?: {
            SliderDefaults.Thumb(
                interactionSource = source,
                colors = sliderColors,
                enabled = enabled,
            )
        },
        track = track ?: { sliderState ->
            SliderDefaults.Track(colors = sliderColors, enabled = enabled, sliderState = sliderState)
        },
    )
}
