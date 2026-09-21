package eu.kanade.tachiyomi.ui.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.library.DeleteLibraryMangaDialog
import eu.kanade.presentation.library.LibrarySettingsDialog
import eu.kanade.presentation.library.components.LibraryContent
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.library.components.SyncFavoritesConfirmDialog
import eu.kanade.presentation.library.components.SyncFavoritesWarningDialog
import eu.kanade.presentation.manga.components.LibraryBottomActionMenu
import eu.kanade.presentation.more.onboarding.GETTING_STARTED_URL
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast
import exh.recs.RecommendsScreen
import exh.recs.batch.RecSearchBottomSheetDialog
import exh.recs.batch.SearchStatus
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.launch
import mihon.feature.migration.config.MigrationConfigScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.isLocal

// The sections of [LibraryTab]: toolbar, selection bottom bar, the library body, dialogs and side effects.

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

@Composable
internal fun LibraryTabBody(
    screenModel: LibraryScreenModel,
    state: LibraryScreenModel.State,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onClickRefresh: (Category?) -> Boolean,
) {
    when {
        state.isLoading -> {
            LoadingScreen(Modifier.padding(contentPadding))
        }
        state.searchQuery.isNullOrEmpty() && !state.hasActiveFilters && state.isLibraryEmpty -> {
            val handler = LocalUriHandler.current
            EmptyScreen(
                stringRes = MR.strings.information_empty_library,
                modifier = Modifier.padding(contentPadding),
                actions = listOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.getting_started_guide,
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = { handler.openUri(GETTING_STARTED_URL) },
                    ),
                ),
            )
        }
        else -> {
            LibraryTabList(screenModel, state, contentPadding, snackbarHostState, onClickRefresh)
        }
    }
}

@Composable
private fun LibraryTabList(
    screenModel: LibraryScreenModel,
    state: LibraryScreenModel.State,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onClickRefresh: (Category?) -> Boolean,
) {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val onContinueReading: (LibraryManga) -> Unit = { libraryManga ->
        scope.launchIO {
            val chapter = screenModel.getNextUnreadChapter(libraryManga.manga)
            if (chapter != null) {
                context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
            } else {
                snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_chapter))
            }
        }
    }
    LibraryContent(
        categories = state.displayedCategories,
        searchQuery = state.searchQuery,
        selection = state.selection,
        contentPadding = contentPadding,
        currentPage = state.coercedActiveCategoryIndex,
        hasActiveFilters = state.hasActiveFilters,
        showPageTabs = state.showCategoryTabs || !state.searchQuery.isNullOrEmpty(),
        onChangeCurrentPage = screenModel::updateActiveCategoryIndex,
        onClickManga = { navigator.push(MangaScreen(it)) },
        onContinueReadingClicked = onContinueReading.takeIf { state.showMangaContinueButton },
        onToggleSelection = screenModel::toggleSelection,
        onToggleRangeSelection = { category, manga ->
            screenModel.toggleRangeSelection(category, manga)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onRefresh = { onClickRefresh(state.activeCategory) },
        onGlobalSearchClicked = { navigator.push(GlobalSearchScreen(screenModel.state.value.searchQuery ?: "")) },
        getItemCountForCategory = { state.getItemCountForCategory(it) },
        getDisplayMode = { screenModel.getDisplayMode() },
        getColumnsForOrientation = { screenModel.getColumnsForOrientation(it) },
        getItemsForCategory = { state.getItemsForCategory(it) },
    )
}

@Composable
internal fun LibraryTabDialogs(
    screenModel: LibraryScreenModel,
    settingsScreenModel: LibrarySettingsScreenModel,
    state: LibraryScreenModel.State,
) {
    val navigator = LocalNavigator.currentOrThrow
    val onDismissRequest = screenModel::closeDialog
    when (val dialog = state.dialog) {
        is LibraryScreenModel.Dialog.SettingsSheet -> {
            LibrarySettingsDialog(
                onDismissRequest = onDismissRequest,
                screenModel = settingsScreenModel,
                category = state.activeCategory,
                // SY -->
                hasCategories = state.libraryData.categories.fastAny { !it.isSystemCategory },
                // SY <--
            )
        }
        is LibraryScreenModel.Dialog.ChangeCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = onDismissRequest,
                onEditCategories = {
                    screenModel.clearSelection()
                    navigator.push(CategoryScreen())
                },
                onConfirm = { include, exclude ->
                    screenModel.clearSelection()
                    screenModel.setMangaCategories(dialog.manga, include, exclude)
                },
            )
        }
        is LibraryScreenModel.Dialog.DeleteManga -> {
            DeleteLibraryMangaDialog(
                containsLocalManga = dialog.manga.any(Manga::isLocal),
                onDismissRequest = onDismissRequest,
                onConfirm = { deleteManga, deleteChapter ->
                    screenModel.removeMangas(dialog.manga, deleteManga, deleteChapter)
                    screenModel.clearSelection()
                },
            )
        }
        // SY -->
        is LibraryScreenModel.Dialog.SyncFavoritesWarning,
        is LibraryScreenModel.Dialog.SyncFavoritesConfirm,
        is LibraryScreenModel.Dialog.RecommendationSearchSheet,
        -> {
            SyDialogs(screenModel, dialog, onDismissRequest)
        }
        // SY <--
        null -> {}
    }
}

// SY --> The fork's favourites-sync and recommendation-search dialogs.
@Composable
private fun SyDialogs(
    screenModel: LibraryScreenModel,
    dialog: LibraryScreenModel.Dialog,
    onDismissRequest: () -> Unit,
) {
    when (dialog) {
        LibraryScreenModel.Dialog.SyncFavoritesWarning -> {
            SyncFavoritesWarningDialog(
                onDismissRequest = onDismissRequest,
                onAccept = {
                    onDismissRequest()
                    screenModel.onAcceptSyncWarning()
                },
            )
        }
        LibraryScreenModel.Dialog.SyncFavoritesConfirm -> {
            SyncFavoritesConfirmDialog(
                onDismissRequest = onDismissRequest,
                onAccept = {
                    onDismissRequest()
                    screenModel.runSync()
                },
            )
        }
        is LibraryScreenModel.Dialog.RecommendationSearchSheet -> {
            RecSearchBottomSheetDialog(
                onDismissRequest = onDismissRequest,
                onSearchRequest = {
                    onDismissRequest()
                    screenModel.clearSelection()
                    screenModel.runRecommendationSearch(dialog.manga)
                },
            )
        }
        else -> {}
    }
}
// SY <--

// SY -->
// A finished batch recommendation search opens its results; a cancelled one is torn down.
@Composable
internal fun RecommendationResultEffect(screenModel: LibraryScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val recSearchState by screenModel.recommendationSearch.status.collectAsState()
    androidx.compose.runtime.LaunchedEffect(recSearchState) {
        when (val current = recSearchState) {
            is SearchStatus.Finished.WithResults -> {
                RecommendsScreen.Args.MergedSourceMangas(current.results)
                    .let(::RecommendsScreen)
                    .let(navigator::push)
                screenModel.recommendationSearch.status.value = SearchStatus.Idle
            }
            is SearchStatus.Finished.WithoutResults -> {
                context.toast(SYMR.strings.rec_no_results)
                screenModel.recommendationSearch.status.value = SearchStatus.Idle
            }
            is SearchStatus.Cancelling -> {
                screenModel.cancelRecommendationSearch()
                screenModel.recommendationSearch.status.value = SearchStatus.Idle
            }
            else -> {}
        }
    }
}
// SY <--
