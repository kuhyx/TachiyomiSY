package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen.Companion.queryEvent
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen.SearchType
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import mihon.presentation.core.util.collectAsLazyPagingItems
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.lang.launchIO

internal fun BrowseSourceScreen.webViewScreen(source: HttpSource) = WebViewScreen(
    url = source.getHomeUrl(),
    initialTitle = source.name,
    sourceId = source.id,
)

@Composable
internal fun BrowseSourceScreen.BrowseSourceBody(
    screenModel: BrowseSourceScreenModel,
    snackbarHostState: SnackbarHostState,
    paddingValues: PaddingValues,
    onWebViewClick: () -> Unit,
    onLocalSourceHelpClick: () -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
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
        onWebViewClick = onWebViewClick,
        onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
        onLocalSourceHelpClick = onLocalSourceHelpClick,
        onMangaClick = { navigator.push(MangaScreen(it.id, true, smartSearchConfig)) },
        onMangaLongClick = { manga ->
            scope.launchIO {
                val duplicates = screenModel.getDuplicateLibraryManga(manga)
                when {
                    manga.favorite -> screenModel.setDialog(BrowseSourceScreenModel.Dialog.RemoveManga(manga))
                    duplicates.isNotEmpty() -> screenModel.setDialog(
                        BrowseSourceScreenModel.Dialog.AddDuplicateManga(manga, duplicates),
                    )
                    else -> screenModel.addFavorite(manga)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        },
    )
}

// Searches pushed in from outside (a genre tap, a global search) land on this screen's model.
@Composable
internal fun BrowseSourceScreen.SearchQueryEffect(screenModel: BrowseSourceScreenModel) {
    LaunchedEffect(Unit) {
        queryEvent.receiveAsFlow()
            .collectLatest {
                when (it) {
                    is SearchType.Genre -> screenModel.searchGenre(it.txt)
                    is SearchType.Text -> screenModel.search(it.txt)
                }
            }
    }
}
