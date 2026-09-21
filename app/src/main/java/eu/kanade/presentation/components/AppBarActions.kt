package eu.kanade.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun AppBarActions(
    actions: List<AppBar.AppBarAction>,
) {
    var showMenu by remember { mutableStateOf(false) }

    actions.filterIsInstance<AppBar.Action>().map {
        TooltipIconButton(
            title =
            it.title,
            icon = it.icon, onClick = it.onClick, enabled = it.enabled, tint = it.iconTint,
        )
    }

    val overflowActions = actions.filterIsInstance<AppBar.OverflowAction>()
    if (overflowActions.isNotEmpty()) {
        TooltipIconButton(
            title = stringResource(MR.strings.action_menu_overflow_description),
            icon = Icons.Outlined.MoreVert,
            onClick = { showMenu = !showMenu },
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            overflowActions.map {
                DropdownMenuItem(
                    onClick = {
                        it.onClick()
                        showMenu = false
                    },
                    text = { Text(it.title, fontWeight = FontWeight.Normal) },
                )
            }
        }
    }
}

// An icon button whose title doubles as its tooltip and content description.
@Composable
internal fun TooltipIconButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null,
) {
    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                Text(title)
            }
        },
        state = rememberTooltipState(),
        focusable = false,
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = icon,
                tint = tint ?: LocalContentColor.current,
                contentDescription = title,
            )
        }
    }
}
