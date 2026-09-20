package tachiyomi.presentation.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.toggle
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.collectAsState

private val SortIconSize: Dp = 24.dp

/** The inset every settings row shares. */
public object SettingsItemsPaddings {
    /** Space between a row's content and the screen's side edges. */
    public val Horizontal: Dp = 24.dp

    /** Space above and below a row's content. */
    public val Vertical: Dp = 10.dp
}

/** A section heading from a string resource. */
@Composable
public fun HeadingItem(labelRes: StringResource) {
    HeadingItem(stringResource(labelRes))
}

/** A section heading. */
@Composable
public fun HeadingItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.header,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
    )
}

/** A row with a primary-tinted [icon] before its [label]. */
@Composable
public fun IconItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    BaseSettingsItem(
        label = label,
        content = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        onClick = onClick,
    )
}

/** A sort row: an arrow shows the direction, or nothing while [sortDescending] is `null`. */
@Composable
public fun SortItem(label: String, sortDescending: Boolean?, onClick: () -> Unit) {
    val arrowIcon = sortDescending?.let { if (it) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward }

    BaseSortItem(
        label = label,
        icon = arrowIcon,
        onClick = onClick,
    )
}

/** A sort row with an arbitrary [icon], or an icon-sized gap when it is `null`. */
@Composable
public fun BaseSortItem(label: String, icon: ImageVector?, onClick: () -> Unit) {
    BaseSettingsItem(
        label = label,
        content = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Spacer(modifier = Modifier.size(SortIconSize))
            }
        },
        onClick = onClick,
    )
}

/** A checkbox row bound to [pref]: tapping toggles it. */
@Composable
public fun CheckboxItem(label: String, pref: Preference<Boolean>) {
    val checked by pref.collectAsState()
    val currentPref = rememberUpdatedState(pref)
    CheckboxItem(
        label = label,
        checked = checked,
        onClick = { currentPref.value.toggle() },
    )
}

/** A checkbox row. */
@Composable
public fun CheckboxItem(label: String, checked: Boolean, onClick: () -> Unit) {
    BaseSettingsItem(
        label = label,
        content = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
        },
        onClick = onClick,
    )
}

/** A radio-button row. */
@Composable
public fun RadioItem(label: String, selected: Boolean, onClick: () -> Unit) {
    BaseSettingsItem(
        label = label,
        content = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
        onClick = onClick,
    )
}

/** A row with a painted [icon], tinted primary while [selected]. */
@Composable
public fun IconItem(
    label: String,
    icon: Painter,
    selected: Boolean,
    onClick: () -> Unit,
) {
    BaseSettingsItem(
        label = label,
        content = {
            Icon(
                painter = icon,
                contentDescription = label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        onClick = onClick,
    )
}

/** A clickable row: [content] at the start, [label] beside it. */
@Composable
internal fun BaseSettingsItem(
    label: String,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SettingsItemsPaddings.Horizontal),
    ) {
        content(this)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
