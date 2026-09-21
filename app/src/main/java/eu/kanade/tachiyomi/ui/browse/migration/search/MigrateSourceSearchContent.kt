package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.browse.source.browse.resetFilters
import eu.kanade.tachiyomi.ui.browse.source.browse.search
import eu.kanade.tachiyomi.ui.browse.source.browse.setFilters
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.launch
import mihon.feature.migration.dialog.MigrateMangaDialog
import mihon.feature.migration.list.MigrationListScreen
import mihon.presentation.core.util.collectAsLazyPagingItems
import tachiyomi.core.common.Constants
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalSource

@Composable
internal fun MigrateSourceSearchScreen.MigrateSourceResults(
    screenModel: BrowseSourceScreenModel,
    snackbarHostState: SnackbarHostState,
    paddingValues: PaddingValues,
) {
    val uriHandler = LocalUriHandler.current
    val navigator = LocalNavigator.currentOrThrow
    val openMigrateDialog: (Manga) -> Unit = {
        val migrateListScreen = navigator.items
            .filterIsInstance<MigrationListScreen>()
            .lastOrNull()
        if (migrateListScreen == null) {
            screenModel.setDialog(BrowseSourceScreenModel.Dialog.Migrate(target = it, current = currentManga))
        } else {
            migrateListScreen.addMatchOverride(current = currentManga.id, target = it.id)
            navigator.popUntil { screen -> screen is MigrationListScreen }
        }
    }
    BrowseSourceContent(
        source = screenModel.source,
        mangaList = screenModel.mangaPagerFlowFlow.collectAsLazyPagingItems(),
        columns = screenModel.getColumnsPreference(LocalConfiguration.current.orientation),
        // SY -->
        ehentaiBrowseDisplayMode = screenModel.ehentaiBrowseDisplayMode,
        // SY <--
        displayMode = screenModel.displayMode,
        snackbarHostState = snackbarHostState,
        contentPadding = paddingValues,
        onWebViewClick = {
            val source = screenModel.source as? HttpSource
            if (source != null) {
                navigator.push(
                    WebViewScreen(
                        url =
                        source.getHomeUrl(),
                        initialTitle = source.name, sourceId = source.id,
                    ),
                )
            }
        },
        onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
        onLocalSourceHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) },
        onMangaClick = openMigrateDialog,
        onMangaLongClick = { navigator.push(MangaScreen(it.id, true)) },
    )
}

@Composable
internal fun MigrateSourceSearchScreen.MigrateSourceDialogs(
    screenModel: BrowseSourceScreenModel,
    dialog: BrowseSourceScreenModel.Dialog?,
) {
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()
    val onDismissRequest = { screenModel.setDialog(null) }
    when (dialog) {
        is BrowseSourceScreenModel.Dialog.Filter -> {
            val state by screenModel.state.collectAsState()
            SourceFilterDialog(
                onDismissRequest = onDismissRequest,
                filters = state.filters,
                onReset = screenModel::resetFilters,
                onFilter = { screenModel.search(filters = state.filters) },
                onUpdate = screenModel::setFilters,
                // SY -->
                startExpanded = screenModel.startExpanded,
                onSave = {},
                savedSearches = emptyList(),
                onSavedSearch = {},
                onSavedSearchPress = {},
                openMangaDexRandom = null,
                openMangaDexFollows = null,
                // SY <--
            )
        }
        is BrowseSourceScreenModel.Dialog.Migrate -> {
            MigrateMangaDialog(
                current = currentManga,
                target = dialog.target,
                // Initiated from the context of [currentManga] so we show [dialog.target].
                onClickTitle = { navigator.push(MangaScreen(dialog.target.id)) },
                onDismissRequest = onDismissRequest,
                onComplete = {
                    scope.launch {
                        navigator.popUntilRoot()
                        HomeScreen.openTab(HomeScreen.Tab.Browse())
                        navigator.push(MangaScreen(dialog.target.id))
                    }
                },
            )
        }
        else -> {}
    }
}
