package tachiyomi.presentation.core.components.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.presentation.core.util.animateElevation

/**
 * Represents the elevation for a button in different states.
 *
 * - See [ButtonDefaults.buttonElevation] for the default elevation used in a [Button].
 * - See [ButtonDefaults.flatElevation] for the elevation used in a [TextButton].
 */
@Stable
public class ButtonElevation internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val disabledElevation: Dp,
) {
    /**
     * Represents the tonal elevation used in a button, depending on its [enabled] state and
     * [interactionSource]. This should typically be the same value as the [shadowElevation].
     *
     * Tonal elevation is used to apply a color shift to the surface to give the it higher emphasis.
     * When surface's color is the scheme's surface, a higher elevation will result in a darker
     * color in light theme and lighter color in dark theme.
     *
     * See [shadowElevation] which controls the elevation of the shadow drawn around the button.
     *
     * @param enabled whether the button is enabled
     * @param interactionSource the [InteractionSource] for this button
     */
    @Composable
    internal fun tonalElevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp> =
        animateElevation(enabled = enabled, interactionSource = interactionSource)

    /**
     * Represents the shadow elevation used in a button, depending on its [enabled] state and
     * [interactionSource]. This should typically be the same value as the [tonalElevation].
     *
     * Shadow elevation is used to apply a shadow around the button to give it higher emphasis.
     *
     * See [tonalElevation] which controls the elevation with a color shift to the surface.
     *
     * @param enabled whether the button is enabled
     * @param interactionSource the [InteractionSource] for this button
     */
    @Composable
    internal fun shadowElevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp> =
        animateElevation(enabled = enabled, interactionSource = interactionSource)

    @Composable
    private fun animateElevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.onEach { interaction -> interactions.track(interaction) }.launchIn(this)
        }

        val interaction = interactions.lastOrNull()
        val target = if (!enabled) disabledElevation else elevationFor(interaction)
        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        if (!enabled) {
            // No transition when moving to a disabled state
            LaunchedEffect(target) { animatable.snapTo(target) }
        } else {
            LaunchedEffect(target) {
                animatable.animateElevation(
                    from = interactionFor(animatable.targetValue),
                    to = interaction,
                    target = target,
                )
            }
        }

        return animatable.asState()
    }

    private fun elevationFor(interaction: Interaction?): Dp = when (interaction) {
        is PressInteraction.Press -> pressedElevation
        is HoverInteraction.Enter -> hoveredElevation
        is FocusInteraction.Focus -> focusedElevation
        else -> defaultElevation
    }

    private fun interactionFor(elevation: Dp): Interaction? = when (elevation) {
        pressedElevation -> PressInteraction.Press(Offset.Zero)
        hoveredElevation -> HoverInteraction.Enter()
        focusedElevation -> FocusInteraction.Focus()
        else -> null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ButtonElevation &&
            defaultElevation == other.defaultElevation &&
            pressedElevation == other.pressedElevation &&
            focusedElevation == other.focusedElevation &&
            hoveredElevation == other.hoveredElevation &&
            disabledElevation == other.disabledElevation
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = HASH_MULTIPLIER * result + pressedElevation.hashCode()
        result = HASH_MULTIPLIER * result + focusedElevation.hashCode()
        result = HASH_MULTIPLIER * result + hoveredElevation.hashCode()
        result = HASH_MULTIPLIER * result + disabledElevation.hashCode()
        return result
    }
}

private const val HASH_MULTIPLIER = 31

/** Adds a beginning interaction and removes the one an ending interaction refers to. */
internal fun MutableList<Interaction>.track(interaction: Interaction) {
    when (interaction) {
        is HoverInteraction.Enter -> add(interaction)
        is HoverInteraction.Exit -> remove(interaction.enter)
        is FocusInteraction.Focus -> add(interaction)
        is FocusInteraction.Unfocus -> remove(interaction.focus)
        is PressInteraction.Press -> add(interaction)
        is PressInteraction.Release -> remove(interaction.press)
        is PressInteraction.Cancel -> remove(interaction.press)
    }
}
