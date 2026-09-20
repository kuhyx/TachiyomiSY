package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults as M3ButtonDefaults

/**
 * TextButton with additional onLongClick functionality.
 *
 * Every nullable parameter falls back to its Material default when `null`; [elevation]'s
 * fallback is flat ([ButtonDefaults.flatElevation]).
 *
 * @see androidx.compose.material3.TextButton
 */
@Composable
public fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    elevation: ButtonElevation? = null,
    shape: Shape? = null,
    border: BorderStroke? = null,
    colors: ButtonColors? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        enabled = enabled,
        interactionSource = interactionSource,
        elevation = elevation ?: ButtonDefaults.flatElevation(),
        shape = shape,
        border = border,
        colors = colors ?: ButtonDefaults.textButtonColors(),
        contentPadding = contentPadding ?: M3ButtonDefaults.TextButtonContentPadding,
        content = content,
    )
}

/**
 * Button with additional onLongClick functionality.
 *
 * Every nullable parameter falls back to its Material default when `null`.
 *
 * @see androidx.compose.material3.Button
 */
@Composable
public fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    elevation: ButtonElevation? = null,
    shape: Shape? = null,
    border: BorderStroke? = null,
    colors: ButtonColors? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val buttonColors = colors ?: ButtonDefaults.buttonColors()
    val containerColor = buttonColors.containerColor(enabled).value
    val contentColor = buttonColors.contentColor(enabled).value
    val shadowElevation = (elevation ?: ButtonDefaults.buttonElevation()).shadowElevation(enabled, source).value

    Surface(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        shape = shape ?: M3ButtonDefaults.textShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        border = border,
        interactionSource = source,
        enabled = enabled,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                Row(
                    Modifier.defaultMinSize(
                        minWidth = M3ButtonDefaults.MinWidth,
                        minHeight = M3ButtonDefaults.MinHeight,
                    )
                        .padding(contentPadding ?: M3ButtonDefaults.ContentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

private const val DISABLED_CONTAINER_ALPHA = 0.12f
private val HoveredElevation: Dp = 1.dp

/** Colours and elevations of [Button] and [TextButton]. */
public object ButtonDefaults {
    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [Button]; every `Unspecified` colour takes its Material default.
     *
     * @param containerColor the container color of this [Button] when enabled.
     * @param contentColor the content color of this [Button] when enabled.
     * @param disabledContainerColor the container color of this [Button] when not enabled.
     * @param disabledContentColor the content color of this [Button] when not enabled.
     */
    @Composable
    public fun buttonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors = ButtonColors(
        containerColor = containerColor.takeOrElse { MaterialTheme.colorScheme.primary },
        contentColor = contentColor.takeOrElse { MaterialTheme.colorScheme.onPrimary },
        disabledContainerColor = disabledContainerColor.takeOrElse {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
        },
        disabledContentColor = disabledContentColor.takeOrElse {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
        },
    )

    /** The transparent colours of a [TextButton]. */
    @Composable
    public fun textButtonColors(): ButtonColors = ButtonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
    )

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [Button].
     *
     * @param defaultElevation the elevation used when the [Button] is enabled, and has no other
     * interactions.
     * @param pressedElevation the elevation used when this [Button] is enabled and pressed.
     * @param focusedElevation the elevation used when the [Button] is enabled and focused.
     * @param hoveredElevation the elevation used when the [Button] is enabled and hovered.
     * @param disabledElevation the elevation used when the [Button] is not enabled.
     */
    public fun buttonElevation(
        defaultElevation: Dp = 0.dp,
        pressedElevation: Dp = 0.dp,
        focusedElevation: Dp = 0.dp,
        hoveredElevation: Dp = HoveredElevation,
        disabledElevation: Dp = 0.dp,
    ): ButtonElevation = ButtonElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
        disabledElevation = disabledElevation,
    )

    /** No shadow in any state, as a [TextButton] has. */
    public fun flatElevation(): ButtonElevation = buttonElevation(hoveredElevation = 0.dp)
}
