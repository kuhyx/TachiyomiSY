package mihon.feature.migration.list

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import mihon.feature.migration.list.models.MigratingManga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

// Lift the dropdown over the icon button that opened it.
private const val MENU_ANCHOR_HEIGHT = 56

@Composable
internal fun MigrationListItemAction(
    modifier: Modifier,
    result: MigratingManga.SearchResult,
    onSearchManually: () -> Unit,
    onSkip: () -> Unit,
    onMigrate: () -> Unit,
    onCopy: () -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val closeMenu = { menuExpanded = false }
    Box(modifier) {
        if (result.isSearching()) {
            IconButton(onClick = onSkip) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                )
            }
        } else {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null,
                )
            }
            ActionMenu(
                expanded = menuExpanded,
                closeMenu = closeMenu,
                canMigrate = result is MigratingManga.SearchResult.Success,
                onSearchManually = onSearchManually,
                onSkip = onSkip,
                onMigrate = onMigrate,
                onCopy = onCopy,
            )
        }
    }
}

// Plain so the exhaustive `when` stays out of Compose; a new result kind still fails to compile here.
private fun MigratingManga.SearchResult.isSearching(): Boolean = when (this) {
    MigratingManga.SearchResult.Searching -> true
    MigratingManga.SearchResult.NotFound, is MigratingManga.SearchResult.Success -> false
}

@Composable
internal fun ActionMenu(
    expanded: Boolean,
    closeMenu: () -> Unit,
    canMigrate: Boolean,
    onSearchManually: () -> Unit,
    onSkip: () -> Unit,
    onMigrate: () -> Unit,
    onCopy: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = closeMenu,
        offset = DpOffset(8.dp, (-MENU_ANCHOR_HEIGHT).dp),
    ) {
        MenuEntry(MR.strings.migrationListScreen_searchManuallyActionLabel, closeMenu, onSearchManually)
        MenuEntry(MR.strings.migrationListScreen_skipActionLabel, closeMenu, onSkip)
        if (canMigrate) {
            MenuEntry(MR.strings.migrationListScreen_migrateNowActionLabel, closeMenu, onMigrate)
            MenuEntry(MR.strings.migrationListScreen_copyNowActionLabel, closeMenu, onCopy)
        }
    }
}

@Composable
internal fun MenuEntry(label: StringResource, closeMenu: () -> Unit, action: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = {
            closeMenu()
            action()
        },
    )
}
