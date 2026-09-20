package tachiyomi.presentation.core.components.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color

/**
 * Represents the container and content colors used in a button in different states.
 *
 * - See [ButtonDefaults.buttonColors] for the default colors used in a [Button].
 * - See [ButtonDefaults.textButtonColors] for the default colors used in a [TextButton].
 */
@Immutable
public class ButtonColors internal constructor(
    private val containerColor: Color,
    private val contentColor: Color,
    private val disabledContainerColor: Color,
    private val disabledContentColor: Color,
) {
    /**
     * Represents the container color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    internal fun containerColor(enabled: Boolean): State<Color> =
        rememberUpdatedState(if (enabled) containerColor else disabledContainerColor)

    /**
     * Represents the content color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    internal fun contentColor(enabled: Boolean): State<Color> =
        rememberUpdatedState(if (enabled) contentColor else disabledContentColor)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ButtonColors &&
            containerColor == other.containerColor &&
            contentColor == other.contentColor &&
            disabledContainerColor == other.disabledContainerColor &&
            disabledContentColor == other.disabledContentColor
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = HASH_MULTIPLIER * result + contentColor.hashCode()
        result = HASH_MULTIPLIER * result + disabledContainerColor.hashCode()
        result = HASH_MULTIPLIER * result + disabledContentColor.hashCode()
        return result
    }
}

private const val HASH_MULTIPLIER = 31
