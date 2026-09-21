package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.MissingSourceScreen
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreen
import exh.ui.ifSourcesLoaded
import kotlinx.coroutines.channels.Channel
import tachiyomi.domain.source.model.StubSource
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.LocalSource

internal data class BrowseSourceScreen(
    val sourceId: Long,
    private val listingQuery: String?,
    // SY -->
    private val filtersJson: String? = null,
    private val savedSearch: Long? = null,
    internal val smartSearchConfig: SourcesScreen.SmartSearchConfig? = null,
    // SY <--
) : Screen(), AssistContentScreen {

    private val assistUrl = mutableStateOf<String?>(null)

    override fun onProvideAssistUrl() = assistUrl.value

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }
        val screenModel = rememberScreenModel {
            BrowseSourceScreenModel(
                sourceId = sourceId,
                listingQuery = listingQuery,
                // SY -->
                filtersJson = filtersJson,
                savedSearch = savedSearch,
                // SY <--
            )
        }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val navigateUp: () -> Unit = {
            if (!state.isUserQuery && state.toolbarQuery != null) {
                screenModel.setToolbarQuery(null)
            } else {
                navigator.pop()
            }
        }
        if (screenModel.source is StubSource) {
            MissingSourceScreen(source = screenModel.source, navigateUp = navigateUp)
            return
        }

        val uriHandler = LocalUriHandler.current
        val snackbarHostState = remember { SnackbarHostState() }
        val onHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) }
        val onWebViewClick: () -> Unit =
            { (screenModel.source as? HttpSource)?.let { navigator.push(webViewScreen(it)) } }
        LaunchedEffect(screenModel.source) {
            assistUrl.value = (screenModel.source as? HttpSource)?.getHomeUrl()
        }

        Scaffold(
            topBar = { BrowseSourceTopBar(screenModel, state, navigator, navigateUp, onWebViewClick, onHelpClick) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            BrowseSourceBody(screenModel, snackbarHostState, paddingValues, onWebViewClick, onHelpClick)
        }
        BrowseSourceDialogs(screenModel, state, navigator, LocalContext.current)
        SearchQueryEffect(screenModel)
    }

    companion object {
        internal val queryEvent = Channel<SearchType>()
    }

    sealed class SearchType(val txt: String) {
        class Text(txt: String) : SearchType(txt)
        class Genre(txt: String) : SearchType(txt)
    }
}

internal suspend fun BrowseSourceScreen.search(query: String) =
    BrowseSourceScreen.queryEvent.send(BrowseSourceScreen.SearchType.Text(query))

internal suspend fun BrowseSourceScreen.searchGenre(name: String) =
    BrowseSourceScreen.queryEvent.send(BrowseSourceScreen.SearchType.Genre(name))
