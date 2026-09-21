package eu.kanade.presentation.updates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import eu.kanade.tachiyomi.ui.updates.getUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun UpdateScreen(
    state: UpdatesScreenModel.State,
    snackbarHostState: SnackbarHostState,
    lastUpdated: Long,
    // SY -->
    preserveReadingPosition: Boolean,
    // SY <--
    onClickCover: (UpdatesItem) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onCalendarClicked: () -> Unit,
    onUpdateLibrary: () -> Boolean,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onOpenChapter: (UpdatesItem) -> Unit,
    onFilterClicked: () -> Unit,
    hasActiveFilters: Boolean,
) {
    BackHandler(enabled = state.selectionMode) {
        onSelectAll(false)
    }

    Scaffold(
        topBar = { scrollBehavior ->
            UpdatesAppBar(
                onCalendarClicked = { onCalendarClicked() },
                onUpdateLibrary = { onUpdateLibrary() },
                onFilterClicked = { onFilterClicked() },
                hasFilters = hasActiveFilters,
                actionModeCounter = state.selected.size,
                onSelectAll = { onSelectAll(true) },
                onInvertSelection = { onInvertSelection() },
                onCancelActionMode = { onSelectAll(false) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            UpdatesBottomBar(
                selected = state.selected,
                onDownloadChapter = onDownloadChapter,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMultiDeleteClicked = onMultiDeleteClicked,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        when {
            state.isLoading -> {
                LoadingScreen(Modifier.padding(contentPadding))
            }
            state.items.isEmpty() -> {
                EmptyScreen(
                    stringRes = MR.strings.information_no_recent,
                    modifier = Modifier.padding(contentPadding),
                )
            }
            else -> {
                UpdatesList(
                    state = state,
                    lastUpdated = lastUpdated,
                    preserveReadingPosition = preserveReadingPosition,
                    contentPadding = contentPadding,
                    onUpdateLibrary = onUpdateLibrary,
                    onUpdateSelected = onUpdateSelected,
                    onClickCover = onClickCover,
                    onOpenChapter = onOpenChapter,
                    onDownloadChapter = onDownloadChapter,
                )
            }
        }
    }
}

@Composable
private fun UpdatesList(
    state: UpdatesScreenModel.State,
    lastUpdated: Long,
    // SY -->
    preserveReadingPosition: Boolean,
    // SY <--
    contentPadding: PaddingValues,
    onUpdateLibrary: () -> Boolean,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onOpenChapter: (UpdatesItem) -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    PullRefresh(
        refreshing = isRefreshing,
        onRefresh = {
            val started = onUpdateLibrary()
            if (started) {
                scope.launch {
                    // Fake refresh status but hide it after a second as it's a long running task
                    isRefreshing = true
                    delay(1.seconds)
                    isRefreshing = false
                }
            }
        },
        enabled = !state.selectionMode,
        indicatorPadding = contentPadding,
    ) {
        FastScrollLazyColumn(
            contentPadding = contentPadding,
        ) {
            updatesLastUpdatedItem(lastUpdated)

            updatesUiItems(
                uiModels = state.getUiModel(),
                selectionMode = state.selectionMode,
                // SY -->
                preserveReadingPosition = preserveReadingPosition,
                // SY <--
                onUpdateSelected = onUpdateSelected,
                onClickCover = onClickCover,
                onClickUpdate = onOpenChapter,
                onDownloadChapter = onDownloadChapter,
            )
        }
    }
}

internal sealed interface UpdatesUiModel {
    data class Header(val date: LocalDate) : UpdatesUiModel
    data class Item(val item: UpdatesItem) : UpdatesUiModel
}
