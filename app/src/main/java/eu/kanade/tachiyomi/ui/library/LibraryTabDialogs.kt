package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastAny
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.library.DeleteLibraryMangaDialog
import eu.kanade.presentation.library.LibrarySettingsDialog
import eu.kanade.presentation.library.components.SyncFavoritesConfirmDialog
import eu.kanade.presentation.library.components.SyncFavoritesWarningDialog
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.util.system.toast
import exh.recs.RecommendsScreen
import exh.recs.batch.RecSearchBottomSheetDialog
import exh.recs.batch.SearchStatus
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.sy.SYMR
import tachiyomi.source.local.isLocal

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
internal fun SyDialogs(
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
