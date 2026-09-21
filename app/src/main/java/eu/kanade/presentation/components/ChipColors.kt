package eu.kanade.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Represents the container and content colors used in a clickable chip in different states.
 *
 * See [AssistChipDefaults], [InputChipDefaults], and [SuggestionChipDefaults] for the default
 * colors used in the various Chip configurations.
 */
@ExperimentalMaterial3Api
@Immutable
internal class ChipColors internal constructor(
    private val enabled: ChipStateColors,
    private val disabled: ChipStateColors,
) {
    /**
     * Represents the container color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun containerColor(enabled: Boolean): State<Color> =
        rememberUpdatedState(colors(enabled).container)

    /**
     * Represents the label color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun labelColor(enabled: Boolean): State<Color> =
        rememberUpdatedState(colors(enabled).label)

    /**
     * Represents the leading icon's content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun leadingIconContentColor(enabled: Boolean): State<Color> =
        rememberUpdatedState(colors(enabled).leadingIconContent)

    /**
     * Represents the trailing icon's content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun trailingIconContentColor(enabled: Boolean): State<Color> =
        rememberUpdatedState(colors(enabled).trailingIconContent)

    private fun colors(enabled: Boolean): ChipStateColors = if (enabled) this.enabled else disabled

    override fun equals(other: Any?): Boolean =
        this === other || (other is ChipColors && enabled == other.enabled && disabled == other.disabled)

    override fun hashCode(): Int = 31 * enabled.hashCode() + disabled.hashCode()
}

/** The four colours a chip draws with in one state (enabled or disabled). */
@Immutable
internal data class ChipStateColors(
    val container: Color,
    val label: Color,
    val leadingIconContent: Color,
    val trailingIconContent: Color,
)

/**
 * Represents the border stroke used in a chip in different states.
 */
@ExperimentalMaterial3Api
@Immutable
internal class ChipBorder internal constructor(
    private val borderColor: Color,
    private val disabledBorderColor: Color,
    private val borderWidth: Dp,
) {
    /**
     * Represents the [BorderStroke] for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun borderStroke(enabled: Boolean): State<BorderStroke?> {
        return rememberUpdatedState(
            BorderStroke(borderWidth, if (enabled) borderColor else disabledBorderColor),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ChipBorder) return false

        if (borderColor != other.borderColor) return false
        if (disabledBorderColor != other.disabledBorderColor) return false
        if (borderWidth != other.borderWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = borderColor.hashCode()
        result = 31 * result + disabledBorderColor.hashCode()
        result = 31 * result + borderWidth.hashCode()

        return result
    }
}
