package eu.kanade.presentation.components

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Contains the baseline values used by [SuggestionChip].
 */
@ExperimentalMaterial3Api
internal object SuggestionChipDefaults {

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * a flat [SuggestionChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconContentColor the color of this chip's icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledIconContentColor the color of this chip's icon when not enabled
     */
    @Composable
    fun suggestionChipColors(
        containerColor: Color = Color.Transparent,
        labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor: Color = Color.Transparent,
        disabledLabelColor: Color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.38f),
        disabledIconContentColor: Color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.38f),
    ): ChipColors = ChipColors(
        enabled = ChipStateColors(
            container = containerColor,
            label = labelColor,
            leadingIconContent = iconContentColor,
            trailingIconContent = Color.Unspecified,
        ),
        disabled = ChipStateColors(
            container = disabledContainerColor,
            label = disabledLabelColor,
            leadingIconContent = disabledIconContentColor,
            trailingIconContent = Color.Unspecified,
        ),
    )

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for a flat [SuggestionChip].
     *
     * @param defaultElevation the elevation used when the chip is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun suggestionChipElevation(
        defaultElevation: Dp = 0.0.dp,
        pressedElevation: Dp = defaultElevation,
        focusedElevation: Dp = defaultElevation,
        hoveredElevation: Dp = defaultElevation,
        draggedElevation: Dp = 8.0.dp,
        disabledElevation: Dp = defaultElevation,
    ): ChipElevation = ChipElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
        draggedElevation = draggedElevation,
        disabledElevation = disabledElevation,
    )

    /**
     * Creates a [ChipBorder] that represents the default border used in a flat [SuggestionChip].
     *
     * @param borderColor the border color of this chip when enabled
     * @param disabledBorderColor the border color of this chip when not enabled
     * @param borderWidth the border stroke width of this chip
     */
    @Composable
    fun suggestionChipBorder(
        borderColor: Color = MaterialTheme.colorScheme.outline,
        disabledBorderColor: Color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.12f),
        borderWidth: Dp = 1.0.dp,
    ): ChipBorder = ChipBorder(
        borderColor = borderColor,
        disabledBorderColor = disabledBorderColor,
        borderWidth = borderWidth,
    )

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * an elevated [SuggestionChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconContentColor the color of this chip's icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledIconContentColor the color of this chip's icon when not enabled
     */
    @Composable
    fun elevatedSuggestionChipColors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor: Color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.12f),
        disabledLabelColor: Color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.38f),
        disabledIconContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    ): ChipColors = ChipColors(
        enabled = ChipStateColors(
            container = containerColor,
            label = labelColor,
            leadingIconContent = iconContentColor,
            trailingIconContent = Color.Unspecified,
        ),
        disabled = ChipStateColors(
            container = disabledContainerColor,
            label = disabledLabelColor,
            leadingIconContent = disabledIconContentColor,
            trailingIconContent = Color.Unspecified,
        ),
    )

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for an elevated [SuggestionChip].
     *
     * @param defaultElevation the elevation used when the chip is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun elevatedChipElevation(
        defaultElevation: Dp = 1.0.dp,
        pressedElevation: Dp = 1.0.dp,
        focusedElevation: Dp = 1.0.dp,
        hoveredElevation: Dp = 3.0.dp,
        draggedElevation: Dp = 8.0.dp,
        disabledElevation: Dp = 0.0.dp,
    ): ChipElevation = ChipElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
        draggedElevation = draggedElevation,
        disabledElevation = disabledElevation,
    )
}
