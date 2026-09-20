package mihon.feature.migration.list

import mihon.feature.migration.list.MigrationListScreenModel.Dialog
import mihon.feature.migration.list.models.MigratingManga.SearchResult
import uy.kohesive.injekt.api.get

internal fun MigrationListScreenModel.showMigrateDialog(copy: Boolean) {
    updateState { state ->
        state.copy(
            dialog = Dialog.Migrate(
                copy = copy,
                totalCount = items.size,
                skippedCount = items.count { it.searchResult.value == SearchResult.NotFound },
            ),
        )
    }
}

internal fun MigrationListScreenModel.showExitDialog() {
    updateState {
        it.copy(dialog = Dialog.Exit)
    }
}

internal fun MigrationListScreenModel.dismissDialog() {
    updateState { it.copy(dialog = null) }
}
