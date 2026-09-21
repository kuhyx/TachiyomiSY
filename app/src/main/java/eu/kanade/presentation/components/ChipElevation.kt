package eu.kanade.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import tachiyomi.presentation.core.util.animateElevation

/**
 * Represents the elevation for a chip in different states.
 */
@ExperimentalMaterial3Api
@Immutable
internal class ChipElevation internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val draggedElevation: Dp,
    private val disabledElevation: Dp,
) {
    /**
     * Represents the tonal elevation used in a chip, depending on its [enabled] state and
     * [interactionSource]. This should typically be the same value as the [shadowElevation].
     *
     * Tonal elevation is used to apply a color shift to the surface to give the it higher emphasis.
     * When surface's color is [ColorScheme.surface], a higher elevation will result in a darker
     * color in light theme and lighter color in dark theme.
     *
     * See [shadowElevation] which controls the elevation of the shadow drawn around the chip.
     *
     * @param enabled whether the chip is enabled
     * @param interactionSource the [InteractionSource] for this chip
     */
    @Composable
    internal fun tonalElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> = animateElevation(enabled = enabled, interactionSource = interactionSource)

    /**
     * Represents the shadow elevation used in a chip, depending on its [enabled] state and
     * [interactionSource]. This should typically be the same value as the [tonalElevation].
     *
     * Shadow elevation is used to apply a shadow around the chip to give it higher emphasis.
     *
     * See [tonalElevation] which controls the elevation with a color shift to the surface.
     *
     * @param enabled whether the chip is enabled
     * @param interactionSource the [InteractionSource] for this chip
     */
    @Composable
    internal fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> = animateElevation(enabled = enabled, interactionSource = interactionSource)

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interactions.apply(it) }
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
        is DragInteraction.Start -> draggedElevation
        else -> defaultElevation
    }

    // The interaction an elevation value stands for, so the animation can pick its spec by transition.
    private fun interactionFor(elevation: Dp): Interaction? = when (elevation) {
        pressedElevation -> PressInteraction.Press(Offset.Zero)
        hoveredElevation -> HoverInteraction.Enter()
        focusedElevation -> FocusInteraction.Focus()
        draggedElevation -> DragInteraction.Start()
        else -> null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ChipElevation) return false

        if (defaultElevation != other.defaultElevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        if (hoveredElevation != other.hoveredElevation) return false
        if (disabledElevation != other.disabledElevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        result = 31 * result + disabledElevation.hashCode()
        return result
    }
}

// Keeps the list of active interactions: a start adds itself, its matching end removes the start.
internal fun MutableList<Interaction>.apply(interaction: Interaction) {
    when (interaction) {
        is HoverInteraction.Enter -> add(interaction)
        is HoverInteraction.Exit -> remove(interaction.enter)
        is FocusInteraction.Focus -> add(interaction)
        is FocusInteraction.Unfocus -> remove(interaction.focus)
        is PressInteraction.Press -> add(interaction)
        is PressInteraction.Release -> remove(interaction.press)
        is PressInteraction.Cancel -> remove(interaction.press)
        is DragInteraction.Start -> add(interaction)
        is DragInteraction.Stop -> remove(interaction.start)
        is DragInteraction.Cancel -> remove(interaction.start)
    }
}
