package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.manga.ChapterRowActions
import eu.kanade.presentation.manga.ChapterSelectionActions
import eu.kanade.presentation.manga.MangaHeaderActions
import eu.kanade.presentation.manga.MangaInfoActions
import eu.kanade.presentation.manga.MangaScreen
import eu.kanade.presentation.manga.MangaScreenActions
import eu.kanade.presentation.manga.MangaToolbarActions
import eu.kanade.presentation.manga.MergeActions
import eu.kanade.presentation.manga.PagePreviewActions
import eu.kanade.presentation.manga.SyMangaActions
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.source.isLocalOrStub
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.manga.notes.MangaNotesScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import exh.source.MERGED_SOURCE_ID
import exh.source.getMainSource
import kotlinx.coroutines.launch
import mihon.feature.migration.config.MigrationConfigScreen
import tachiyomi.domain.manga.model.expectedNextUpdate

// The manga screen's presentation call, with every callback wired to the screen model or a navigation helper.
@Composable
internal fun MangaScreen.MangaScreenBody(screenModel: MangaScreenModel, successState: MangaScreenModel.State.Success) {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isHttpSource = remember { successState.source is HttpSource }
    val isFavorite = successState.manga.favorite
    val hasDownloads = !successState.source.isLocalOrStub()
    MangaScreen(
        state = successState,
        snackbarHostState = screenModel.snackbarHostState,
        nextUpdate = successState.manga.expectedNextUpdate,
        isTabletUi = isTabletUi(),
        chapterSwipeStartAction = screenModel.chapterSwipeStartAction,
        chapterSwipeEndAction = screenModel.chapterSwipeEndAction,
        actions = MangaScreenActions(
            toolbar = toolbarActions(screenModel, successState),
            header = MangaHeaderActions(
                onAddToLibraryClicked = {
                    screenModel.toggleFavorite()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                // SY -->
                onWebViewClicked =
                { openWebView(context, navigator, screenModel, successState) }.takeIf { isHttpSource },
                // SY <--
                onWebViewLongClicked = { copyMangaUrl(context, screenModel.manga, screenModel.source) }
                    .takeIf { isHttpSource },
                onTrackingClicked = { openTracking(navigator, screenModel, successState) },
                onEditFetchIntervalClicked = screenModel.library::showSetFetchIntervalDialog.takeIf { isFavorite },
                onEditCategoryClicked = screenModel.library::showChangeCategoryDialog.takeIf { isFavorite },
            ),
            info = MangaInfoActions(
                onCoverClicked = screenModel::showCoverDialog,
                onSearch = { query, global -> scope.launch { performSearch(navigator, query, global) } },
                onTagSearch = { scope.launch { performGenreSearch(navigator, it, screenModel.source!!) } },
                onEditNotesClicked = { navigator.push(MangaNotesScreen(manga = successState.manga)) },
                onContinueReading = { continueReading(context, screenModel.getNextUnreadChapter()) },
            ),
            chapters = ChapterRowActions(
                onChapterClicked = { openChapter(context, it) },
                onDownloadChapter = screenModel.downloads::runChapterDownloadActions.takeIf { hasDownloads },
                onChapterSelected = screenModel::toggleSelection,
                onChapterSwipe = screenModel.chapterActions::chapterSwipe,
            ),
            selection = selectionActions(screenModel),
            // SY -->
            sy = syActions(screenModel, successState),
            // SY <--
        ),
    )
}

@Composable
private fun toolbarActions(
    screenModel: MangaScreenModel,
    successState: MangaScreenModel.State.Success,
): MangaToolbarActions {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val isHttpSource = remember { successState.source is HttpSource }
    val isFavorite = successState.manga.favorite
    val hasDownloads = !successState.source.isLocalOrStub()
    return MangaToolbarActions(
        navigateUp = navigator::pop,
        onFilterButtonClicked = screenModel::showSettingsDialog,
        onShareClicked = { shareManga(context, screenModel.manga, screenModel.source) }.takeIf { isHttpSource },
        onDownloadActionClicked = screenModel.downloads::runDownloadAction.takeIf { hasDownloads },
        onRefresh = screenModel::fetchAllFromSource,
        onMigrateClicked = { navigator.push(MigrationConfigScreen(successState.manga.id)) }.takeIf {
            isFavorite /* SY --> */ && successState.manga.source != MERGED_SOURCE_ID /* SY <-- */
        },
    )
}

private fun selectionActions(screenModel: MangaScreenModel) = ChapterSelectionActions(
    onMultiBookmarkClicked = screenModel.chapterActions::bookmarkChapters,
    onMultiMarkAsReadClicked = screenModel.chapterActions::markChaptersRead,
    onMarkPreviousAsReadClicked = screenModel.chapterActions::markPreviousChapterRead,
    onMultiDeleteClicked = screenModel::showDeleteChapterDialog,
    onAllChapterSelected = screenModel::toggleAllSelection,
    onInvertSelection = screenModel::invertSelection,
)

// SY -->
@Composable
private fun MangaScreen.syActions(
    screenModel: MangaScreenModel,
    successState: MangaScreenModel.State.Success,
): SyMangaActions {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    return SyMangaActions(
        onMetadataViewerClicked = { openMetadataViewer(navigator, successState.manga) },
        onEditInfoClicked = screenModel::showEditMangaInfoDialog,
        onRecommendClicked = { openRecommends(navigator, screenModel.source?.getMainSource(), successState.manga) },
        merge = MergeActions(
            onMergedSettingsClicked = screenModel::showEditMergedSettingsDialog,
            onMergeClicked = { openSmartSearch(navigator, successState.manga) },
            onMergeWithAnotherClicked = {
                mergeWithAnother(navigator, context, successState.manga, screenModel.merger::smartSearchMerge)
            },
        ),
        previews = PagePreviewActions(
            onOpenPagePreview = {
                openPagePreview(context, successState.chapters.minByOrNull { it.chapter.sourceOrder }?.chapter, it)
            },
            onMorePreviewsClicked = { openMorePagePreviews(navigator, successState.manga) },
            previewsRowCount = successState.previewsRowCount,
        ),
    )
}
// SY <--

// SY -->
private fun openWebView(
    context: Context,
    navigator: Navigator,
    screenModel: MangaScreenModel,
    successState: MangaScreenModel.State.Success,
) {
    if (successState.mergedData == null) {
        openMangaInWebView(navigator, screenModel.manga, screenModel.source)
    } else {
        openMergedMangaWebview(context, navigator, successState.mergedData)
    }
}
// SY <--

private fun openTracking(
    navigator: Navigator,
    screenModel: MangaScreenModel,
    successState: MangaScreenModel.State.Success,
) {
    if (!successState.hasLoggedInTrackers) {
        navigator.push(SettingsScreen(SettingsScreen.Destination.Tracking))
    } else {
        screenModel.showTrackDialog()
    }
}
