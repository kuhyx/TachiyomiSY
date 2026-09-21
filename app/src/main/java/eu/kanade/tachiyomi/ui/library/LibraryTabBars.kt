package eu.kanade.tachiyomi.ui.library

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastAll
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.manga.components.LibraryBottomActionMenu
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.launch
import mihon.feature.migration.config.MigrationConfigScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal

@Composable
internal fun LibraryTabToolbar(
    screenModel: LibraryScreenModel,
    state: LibraryScreenModel.State,
    snackbarHostState: SnackbarHostState,
    onClickRefresh: (Category?) -> Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val title = state.getToolbarTitle(
        defaultTitle = stringResource(MR.strings.label_library),
        defaultCategoryTitle = stringResource(MR.strings.label_default),
        page = state.coercedActiveCategoryIndex,
    )
    LibraryToolbar(
        hasActiveFilters = state.hasActiveFilters,
        selectedCount = state.selection.size,
        title = title,
        onClickUnselectAll = screenModel::clearSelection,
        onClickSelectAll = screenModel::selectAll,
        onClickInvertSelection = screenModel::invertSelection,
        onClickFilter = screenModel::showSettingsDialog,
        onClickRefresh = { onClickRefresh(state.activeCategory) },
        onClickGlobalUpdate = { onClickRefresh(null) },
        onClickOpenRandomManga = {
            scope.launch {
                val randomItem = screenModel.randomItemInCurrentCategory()
                if (randomItem != null) {
                    navigator.push(MangaScreen(randomItem.libraryManga.manga.id))
                } else {
                    snackbarHostState.showSnackbar(context.stringResource(MR.strings.information_no_entries_found))
                }
            }
        },
        onClickSyncNow = {
            if (!SyncDataJob.isRunning(context)) {
                SyncDataJob.startNow(context, manual = true)
            } else {
                context.toast(SYMR.strings.sync_in_progress)
            }
        },
        // SY -->
        onClickSyncExh = screenModel::openFavoritesSyncDialog.takeIf { state.showSyncExh },
        isSyncEnabled = state.isSyncEnabled,
        // SY <--
        searchQuery = state.searchQuery,
        onSearchQueryChange = screenModel::search,
        // For scroll overlay when no tab
        scrollBehavior = scrollBehavior.takeIf { !state.showCategoryTabs },
    )
}

@Composable
internal fun LibraryTabBottomBar(screenModel: LibraryScreenModel, state: LibraryScreenModel.State) {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    LibraryBottomActionMenu(
        visible = state.selectionMode,
        onChangeCategoryClicked = screenModel::openChangeCategoryDialog,
        onMarkAsReadClicked = { screenModel.markReadSelection(true) },
        onMarkAsUnreadClicked = { screenModel.markReadSelection(false) },
        onDownloadClicked = screenModel::performDownloadAction
            .takeIf { state.selectedManga.fastAll { !it.isLocal() } },
        onDeleteClicked = screenModel::openDeleteMangaDialog,
        onMigrateClicked = {
            // SY --> merged entries have no source to migrate from
            val selection = state.selectedManga.filterNot { it.source == MERGED_SOURCE_ID }.map { it.id }
            screenModel.clearSelection()
            if (selection.isNotEmpty()) {
                navigator.push(MigrationConfigScreen(selection))
            } else {
                context.toast(SYMR.strings.no_valid_entry)
            }
            // <-- SY
        },
        // SY -->
        onClickCleanTitles = screenModel::cleanTitles.takeIf { state.showCleanTitles },
        onClickCollectRecommendations =
        screenModel::showRecommendationSearchDialog.takeIf { state.selection.size > 1 },
        onClickAddToMangaDex = screenModel::syncMangaToDex.takeIf { state.showAddToMangadex },
        onClickResetInfo = screenModel::resetInfo.takeIf { state.showResetInfo },
        // SY <--
    )
}
