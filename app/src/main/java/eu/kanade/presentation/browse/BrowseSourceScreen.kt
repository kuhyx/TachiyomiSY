package eu.kanade.presentation.browse

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.browse.components.BrowseSourceComfortableGrid
import eu.kanade.presentation.browse.components.BrowseSourceCompactGrid
import eu.kanade.presentation.browse.components.BrowseSourceEHentaiList
import eu.kanade.presentation.browse.components.BrowseSourceList
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.source.Source
import exh.metadata.metadata.RaisedSearchMetadata
import exh.source.isEhBasedSource
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.LocalSource

@Composable
internal fun BrowseSourceContent(
    source: Source?,
    mangaList: LazyPagingItems<StateFlow</* SY --> */Pair<Manga, RaisedSearchMetadata?>/* SY <-- */>>,
    columns: GridCells,
    // SY -->
    ehentaiBrowseDisplayMode: Boolean,
    // SY <--
    displayMode: LibraryDisplayMode,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    // SY -->
    onWebViewClick: (() -> Unit)?,
    onHelpClick: (() -> Unit)?,
    onLocalSourceHelpClick: (() -> Unit)?,
    // SY <--
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    val context = LocalContext.current

    val errorState = mangaList.loadState.refresh as? LoadState.Error
        ?: mangaList.loadState.append as? LoadState.Error

    val getErrorMessage: (LoadState.Error) -> String = { state ->
        with(context) { state.error.formattedMessage }
    }

    // With results already on screen a load error is a retry snackbar rather than an empty screen.
    LaunchedEffect(errorState) {
        if (mangaList.itemCount > 0 && errorState != null) {
            mangaList.showRetrySnackbar(snackbarHostState, getErrorMessage(errorState), context)
        }
    }

    when {
        mangaList.itemCount == 0 && mangaList.loadState.refresh is LoadState.Loading -> {
            LoadingScreen(Modifier.padding(contentPadding))
        }
        mangaList.itemCount == 0 -> {
            NoResultsScreen(
                contentPadding = contentPadding,
                message = errorState?.let(getErrorMessage) ?: stringResource(MR.strings.no_results_found),
                localSourceHelp = onLocalSourceHelpClick.takeIf { source is LocalSource },
                helpActions = emptyHelpActions(mangaList::refresh, onWebViewClick, onHelpClick),
            )
        }
        // SY -->
        source?.isEhBasedSource() == true && ehentaiBrowseDisplayMode -> {
            BrowseSourceEHentaiList(
                mangaList = mangaList,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
        // SY <--
        else -> {
            BrowseSourceItems(
                displayMode = displayMode,
                mangaList = mangaList,
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
    }
}

private suspend fun LazyPagingItems<*>.showRetrySnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
    context: Context,
) {
    val result = snackbarHostState.showSnackbar(
        message = message,
        actionLabel = context.stringResource(MR.strings.action_retry),
        duration = SnackbarDuration.Indefinite,
    )
    when (result) {
        SnackbarResult.Dismissed -> snackbarHostState.currentSnackbarData?.dismiss()
        SnackbarResult.ActionPerformed -> retry()
    }
}

// Retry always; web view and help only when the source offers them.
private fun emptyHelpActions(
    onRetry: () -> Unit,
    onWebViewClick: (() -> Unit)?,
    onHelpClick: (() -> Unit)?,
): List<EmptyScreenAction> {
    return listOfNotNull(
        EmptyScreenAction(
            stringRes = MR.strings.action_retry,
            icon = Icons.Outlined.Refresh,
            onClick = onRetry,
        ),
        // SY -->
        onWebViewClick?.let {
            EmptyScreenAction(
                stringRes = MR.strings.action_open_in_web_view,
                icon = Icons.Outlined.Public,
                onClick = it,
            )
        },
        onHelpClick?.let {
            EmptyScreenAction(
                stringRes = MR.strings.label_help,
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = it,
            )
        },
        // SY <--
    )
}

@Composable
private fun BrowseSourceItems(
    displayMode: LibraryDisplayMode,
    mangaList: LazyPagingItems<StateFlow</* SY --> */Pair<Manga, RaisedSearchMetadata?>/* SY <-- */>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    when (displayMode) {
        LibraryDisplayMode.ComfortableGrid -> {
            BrowseSourceComfortableGrid(
                mangaList = mangaList,
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
        LibraryDisplayMode.List -> {
            BrowseSourceList(
                mangaList = mangaList,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
        LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
            BrowseSourceCompactGrid(
                mangaList = mangaList,
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
    }
}

// A local source with nothing in it gets the setup guide; anything else gets retry / web view / help.
@Composable
private fun NoResultsScreen(
    contentPadding: PaddingValues,
    message: String,
    localSourceHelp: (() -> Unit)?,
    helpActions: List<EmptyScreenAction>,
) {
    EmptyScreen(
        modifier = Modifier.padding(contentPadding),
        message = message,
        actions = if (localSourceHelp != null) {
            listOf(
                EmptyScreenAction(
                    stringRes = MR.strings.local_source_help_guide,
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onClick = localSourceHelp,
                ),
            )
        } else {
            helpActions
        },
    )
}

@Composable
internal fun MissingSourceScreen(
    source: StubSource,
    navigateUp: () -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = source.name,
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        EmptyScreen(
            message = stringResource(MR.strings.source_not_installed, source.toString()),
            modifier = Modifier.padding(paddingValues),
        )
    }
}
