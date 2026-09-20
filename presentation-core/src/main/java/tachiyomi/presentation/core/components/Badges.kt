package tachiyomi.presentation.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BadgeHorizontalPadding: Dp = 3.dp
private val BadgeVerticalPadding: Dp = 1.dp

/**
 * A row of [Badge]s clipped to one [shape].
 *
 * `shape`: the clip shape; the theme's extra-small shape when `null`.
 */
@Composable
public fun BadgeGroup(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier.clip(shape ?: MaterialTheme.shapes.extraSmall)) {
        content()
    }
}

/**
 * A small text label; [color] and [textColor] default to the theme's secondary pair when
 * `Unspecified`.
 */
@Composable
public fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textColor: Color = Color.Unspecified,
    shape: Shape = RectangleShape,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(shape)
            .background(color.takeOrElse { MaterialTheme.colorScheme.secondary })
            .padding(horizontal = BadgeHorizontalPadding, vertical = BadgeVerticalPadding),
        color = textColor.takeOrElse { MaterialTheme.colorScheme.onSecondary },
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
    )
}

/**
 * A small icon label; [color] and [iconColor] default to the theme's secondary pair when
 * `Unspecified`.
 */
@Composable
public fun Badge(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    iconColor: Color = Color.Unspecified,
    shape: Shape = RectangleShape,
) {
    val tint = iconColor.takeOrElse { MaterialTheme.colorScheme.onSecondary }
    val iconContentPlaceholder = "[icon]"
    val text = buildAnnotatedString {
        appendInlineContent(iconContentPlaceholder)
    }
    val inlineContent = mapOf(
        Pair(
            iconContentPlaceholder,
            InlineTextContent(
                Placeholder(
                    width = MaterialTheme.typography.bodySmall.fontSize,
                    height = MaterialTheme.typography.bodySmall.fontSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                Icon(
                    imageVector = imageVector,
                    tint = tint,
                    contentDescription = null,
                )
            },
        ),
    )

    Text(
        text = text,
        inlineContent = inlineContent,
        modifier = modifier
            .clip(shape)
            .background(color.takeOrElse { MaterialTheme.colorScheme.secondary })
            .padding(horizontal = BadgeHorizontalPadding, vertical = BadgeVerticalPadding),
        color = tint,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
    )
}
