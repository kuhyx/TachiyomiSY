package eu.kanade.tachiyomi.ui.manga

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.NavigatorAdaptiveSheet
import eu.kanade.presentation.manga.ChapterSettingsDialog
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.presentation.manga.EditCoverAction
import eu.kanade.presentation.manga.components.DeleteChaptersDialog
import eu.kanade.presentation.manga.components.MangaCoverDialog
import eu.kanade.presentation.manga.components.ScanlatorFilterDialog
import eu.kanade.presentation.manga.components.SetIntervalDialog
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.manga.merged.EditMergedSettingsDialog
import eu.kanade.tachiyomi.ui.manga.track.TrackInfoDialogHomeScreen
import mihon.feature.migration.dialog.MigrateMangaDialog
import tachiyomi.domain.manga.model.expectedNextUpdate
import tachiyomi.presentation.core.screens.LoadingScreen

// The dialog the manga screen model asked for, if any.
@Composable
internal fun MangaScreen.MangaScreenDialogs(
    screenModel: MangaScreenModel,
    successState: MangaScreenModel.State.Success,
) {
    val navigator = LocalNavigator.currentOrThrow
    var showScanlatorsDialog by remember { mutableStateOf(false) }
    val onDismissRequest = { screenModel.dismissDialog() }
    when (val dialog = successState.dialog) {
        null -> {}
        is MangaScreenModel.Dialog.ChangeCategory,
        is MangaScreenModel.Dialog.DuplicateManga,
        is MangaScreenModel.Dialog.Migrate,
        is MangaScreenModel.Dialog.SetFetchInterval,
        -> {
            LibraryDialogs(screenModel, dialog, onDismissRequest)
        }
        is MangaScreenModel.Dialog.DeleteChapters -> {
            DeleteChaptersDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    screenModel.toggleAllSelection(false)
                    screenModel.downloads.deleteChapters(dialog.chapters)
                },
            )
        }
        MangaScreenModel.Dialog.SettingsSheet -> {
            ChapterSettingsSheet(screenModel, successState, onDismissRequest) { showScanlatorsDialog = true }
        }
        MangaScreenModel.Dialog.TrackSheet -> {
            NavigatorAdaptiveSheet(
                screen = TrackInfoDialogHomeScreen(
                    mangaId = successState.manga.id,
                    mangaTitle = successState.manga.title,
                    sourceId = successState.source.id,
                ),
                enableSwipeDismiss = { it.lastItem is TrackInfoDialogHomeScreen },
                onDismissRequest = onDismissRequest,
            )
        }
        MangaScreenModel.Dialog.FullCover -> {
            FullCoverDialog(successState, onDismissRequest)
        }
        // SY -->
        is MangaScreenModel.Dialog.EditMangaInfo,
        is MangaScreenModel.Dialog.EditMergedSettings,
        -> {
            SyDialogs(screenModel, dialog)
        }
        // SY <--
    }

    if (showScanlatorsDialog) {
        ScanlatorFilterDialog(
            availableScanlators = successState.availableScanlators,
            excludedScanlators = successState.excludedScanlators,
            onDismissRequest = { showScanlatorsDialog = false },
            onConfirm = screenModel.chapterSettings::setExcludedScanlators,
        )
    }
}

// Add-to-library flow (categories, duplicate check, migration) and the fetch-interval editor.
@Composable
private fun MangaScreen.LibraryDialogs(
    screenModel: MangaScreenModel,
    dialog: MangaScreenModel.Dialog,
    onDismissRequest: () -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow
    when (dialog) {
        is MangaScreenModel.Dialog.ChangeCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = onDismissRequest,
                onEditCategories = { navigator.push(CategoryScreen()) },
                onConfirm = { include, _ ->
                    screenModel.library.addToLibraryInCategories(dialog.manga, include)
                },
            )
        }
        is MangaScreenModel.Dialog.DuplicateManga -> {
            DuplicateMangaDialog(
                duplicates = dialog.duplicates,
                onDismissRequest = onDismissRequest,
                onConfirm = { screenModel.toggleFavorite(onRemoved = {}, checkDuplicate = false) },
                onOpenManga = { navigator.push(MangaScreen(it.id)) },
                onMigrate = { screenModel.showMigrateDialog(it) },
            )
        }
        is MangaScreenModel.Dialog.Migrate -> {
            MigrateMangaDialog(
                current = dialog.current,
                target = dialog.target,
                // Initiated from the context of [dialog.target] so we show [dialog.current].
                onClickTitle = { navigator.push(MangaScreen(dialog.current.id)) },
                onDismissRequest = onDismissRequest,
            )
        }
        is MangaScreenModel.Dialog.SetFetchInterval -> {
            SetIntervalDialog(
                interval = dialog.manga.fetchInterval,
                nextUpdate = dialog.manga.expectedNextUpdate,
                onDismissRequest = onDismissRequest,
                onValueChanged = { interval: Int -> screenModel.library.setFetchInterval(dialog.manga, interval) }
                    .takeIf { screenModel.isUpdateIntervalEnabled },
            )
        }
        else -> {}
    }
}

// SY --> The fork's metadata and merged-source editors.
@Composable
private fun SyDialogs(screenModel: MangaScreenModel, dialog: MangaScreenModel.Dialog) {
    when (dialog) {
        is MangaScreenModel.Dialog.EditMangaInfo -> {
            EditMangaDialog(
                manga = dialog.manga,
                onDismissRequest = screenModel::dismissDialog,
                onPositiveClick = screenModel::updateMangaInfo,
            )
        }
        is MangaScreenModel.Dialog.EditMergedSettings -> {
            EditMergedSettingsDialog(
                mergedData = dialog.mergedData,
                onDismissRequest = screenModel::dismissDialog,
                onDeleteClick = screenModel::deleteMerge,
                onPositiveClick = screenModel::updateMergeSettings,
            )
        }
        else -> {}
    }
}
// SY <--

@Composable
private fun ChapterSettingsSheet(
    screenModel: MangaScreenModel,
    successState: MangaScreenModel.State.Success,
    onDismissRequest: () -> Unit,
    onScanlatorFilterClicked: () -> Unit,
) {
    ChapterSettingsDialog(
        onDismissRequest = onDismissRequest,
        manga = successState.manga,
        onDownloadFilterChanged = screenModel.chapterSettings::setDownloadedFilter,
        onUnreadFilterChanged = screenModel.chapterSettings::setUnreadFilter,
        onBookmarkedFilterChanged = screenModel.chapterSettings::setBookmarkedFilter,
        onSortModeChanged = screenModel.chapterSettings::setSorting,
        onDisplayModeChanged = screenModel.chapterSettings::setDisplayMode,
        onSetAsDefault = screenModel.chapterSettings::setCurrentSettingsAsDefault,
        onResetToDefault = screenModel.chapterSettings::resetToDefaultSettings,
        scanlatorFilterActive = successState.scanlatorFilterActive,
        onScanlatorFilterClicked = onScanlatorFilterClicked,
    )
}

@Composable
private fun MangaScreen.FullCoverDialog(successState: MangaScreenModel.State.Success, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val sm = rememberScreenModel { MangaCoverScreenModel(successState.manga.id) }
    val manga by sm.state.collectAsState()
    if (manga == null) {
        LoadingScreen(Modifier.systemBarsPadding())
        return
    }
    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            sm.editCover(context, it)
        }
    }
    MangaCoverDialog(
        manga = manga!!,
        snackbarHostState = sm.snackbarHostState,
        isCustomCover = remember(manga) { manga!!.hasCustomCover() },
        onShareClick = { sm.shareCover(context) },
        onSaveClick = { sm.saveCover(context) },
        onEditClick = {
            when (it) {
                EditCoverAction.EDIT -> getContent.launch("image/*")
                EditCoverAction.DELETE -> sm.deleteCustomCover(context)
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
