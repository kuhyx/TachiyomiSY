package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.browse.migration.advanced.process.MigratingManga
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

// Lift the dropdown over the icon button that opened it.
private const val MENU_ANCHOR_HEIGHT = 56

@Composable
internal fun MigrationActionIcon(
    modifier: Modifier,
    result: MigratingManga.SearchResult,
    skipManga: () -> Unit,
    searchManually: () -> Unit,
    migrateNow: () -> Unit,
    copyNow: () -> Unit,
) {
    var moreExpanded by remember { mutableStateOf(false) }
    val closeMenu = { moreExpanded = false }

    Box(modifier) {
        when (result) {
            MigratingManga.SearchResult.Searching -> {
                IconButton(onClick = skipManga) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(SYMR.strings.action_stop),
                    )
                }
            }
            MigratingManga.SearchResult.NotFound, is MigratingManga.SearchResult.Result -> {
                IconButton(onClick = { moreExpanded = !moreExpanded }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(MR.strings.action_menu_overflow_description),
                    )
                }
                DropdownMenu(
                    expanded = moreExpanded,
                    onDismissRequest = closeMenu,
                    offset = DpOffset(8.dp, (-MENU_ANCHOR_HEIGHT).dp),
                ) {
                    MenuItem(SYMR.strings.action_search_manually, searchManually, closeMenu)
                    MenuItem(SYMR.strings.action_skip_entry, skipManga, closeMenu)
                    if (result is MigratingManga.SearchResult.Result) {
                        MenuItem(SYMR.strings.action_migrate_now, migrateNow, closeMenu)
                        MenuItem(SYMR.strings.action_copy_now, copyNow, closeMenu)
                    }
                }
            }
        }
    }
}

// One overflow entry: runs its action, then closes the menu.
@Composable
private fun MenuItem(label: StringResource, action: () -> Unit, closeMenu: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = {
            action()
            closeMenu()
        },
    )
}
