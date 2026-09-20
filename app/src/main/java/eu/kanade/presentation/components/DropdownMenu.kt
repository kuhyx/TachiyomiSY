package eu.kanade.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.material3.DropdownMenu as ComposeDropdownMenu

// DropdownMenu but overlaps anchor and has width constraints to better
// match non-Compose implementation.
// Lift the dropdown over the icon button that opened it.
private const val MENU_ANCHOR_HEIGHT = 56

@Composable
internal fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(8.dp, (-MENU_ANCHOR_HEIGHT).dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ComposeDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.sizeIn(minWidth = 196.dp, maxWidth = 196.dp),
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        content = content,
    )
}

@Composable
internal fun RadioMenuItem(
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    DropdownMenuItem(
        text = content,
        onClick = onClick,
        trailingIcon = {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Outlined.RadioButtonChecked,
                    contentDescription = stringResource(MR.strings.selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = stringResource(MR.strings.not_selected),
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
internal fun NestedMenuItem(
    text: @Composable () -> Unit,
    children: @Composable ColumnScope.(() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nestedExpanded by remember { mutableStateOf(false) }
    val closeMenu = { nestedExpanded = false }

    Box {
        DropdownMenuItem(
            text = text,
            onClick = { nestedExpanded = true },
            trailingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowRight,
                    contentDescription = null,
                )
            },
        )

        DropdownMenu(
            expanded = nestedExpanded,
            onDismissRequest = closeMenu,
            modifier = modifier,
        ) {
            children(closeMenu)
        }
    }
}
