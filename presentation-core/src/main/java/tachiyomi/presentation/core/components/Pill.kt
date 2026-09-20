package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

private val PillStartPadding: Dp = 4.dp
private val PillHorizontalPadding: Dp = 6.dp
private val PillVerticalPadding: Dp = 1.dp

/**
 * A rounded, single-line label. [color] and [contentColor] take the theme's high surface
 * container and on-surface colours when `Unspecified`.
 *
 * `style`: the text style; the current text style when `null`.
 */
@Composable
public fun Pill(
    text: String,
    style: TextStyle?,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    Surface(
        modifier = modifier
            .padding(start = PillStartPadding),
        shape = MaterialTheme.shapes.extraLarge,
        color = color.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHigh },
        contentColor = contentColor.takeOrElse { MaterialTheme.colorScheme.onSurface },
    ) {
        Box(
            modifier = Modifier
                .padding(PillHorizontalPadding, PillVerticalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                maxLines = 1,
                style = style ?: LocalTextStyle.current,
            )
        }
    }
}

/**
 * A [Pill] in the current text style at [fontSize] (the current size when `Unspecified`).
 */
@Composable
public fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    Pill(
        text = text,
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        style = if (fontSize.isSpecified) LocalTextStyle.current.merge(fontSize = fontSize) else LocalTextStyle.current,
    )
}
