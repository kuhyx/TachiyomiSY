package eu.kanade.tachiyomi.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.library.components.SyncFavoritesProgressDialog
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import exh.favorites.FavoritesSyncStatus
import exh.recs.batch.RecSearchProgressDialog
import exh.recs.batch.SearchStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

internal data object LibraryTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_library),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    // For invoking search from other screen
    private val queryEvent = Channel<String>()

    // For opening settings sheet in LibraryController
    private val requestSettingsSheetEvent = Channel<Unit>()

    override suspend fun onReselect(navigator: Navigator) {
        requestOpenSettingsSheet()
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { LibraryScreenModel() }
        val settingsScreenModel = rememberScreenModel { LibrarySettingsScreenModel() }
        val state by screenModel.state.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }

        val onClickRefresh = rememberRefreshAction(state, snackbarHostState)

        Scaffold(
            topBar = { LibraryTabToolbar(screenModel, state, snackbarHostState, onClickRefresh, it) },
            bottomBar = { LibraryTabBottomBar(screenModel, state) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { contentPadding ->
            LibraryTabBody(screenModel, state, contentPadding, snackbarHostState, onClickRefresh)
        }
        LibraryTabDialogs(screenModel, settingsScreenModel, state)

        // SY -->
        SyncFavoritesProgressDialog(
            status = screenModel.favoritesSync.status.collectAsState().value,
            setStatusIdle = { screenModel.favoritesSync.status.value = FavoritesSyncStatus.Idle },
            openManga = { navigator.push(MangaScreen(it)) },
        )
        RecSearchProgressDialog(
            status = screenModel.recommendationSearch.status.collectAsState().value,
            setStatusIdle = { screenModel.recommendationSearch.status.value = SearchStatus.Idle },
            setStatusCancelling = { screenModel.recommendationSearch.status.value = SearchStatus.Cancelling },
        )
        RecommendationResultEffect(screenModel)
        // SY <--

        BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
            when {
                state.selectionMode -> screenModel.clearSelection()
                state.searchQuery != null -> screenModel.search(null)
            }
        }
        LaunchedEffect(state.selectionMode, state.dialog) {
            HomeScreen.showBottomNav(!state.selectionMode)
        }
        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }
        LaunchedEffect(Unit) {
            launch { queryEvent.receiveAsFlow().collect(screenModel::search) }
            launch { requestSettingsSheetEvent.receiveAsFlow().collectLatest { screenModel.showSettingsDialog() } }
        }
    }

    // Starts a library update for [Category] (or everything) and reports the outcome in the snackbar.
    @Composable
    private fun rememberRefreshAction(
        state: LibraryScreenModel.State,
        snackbarHostState: SnackbarHostState,
    ): (Category?) -> Boolean {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        return { category ->
            // SY -->
            val started = LibraryUpdateJob.startNow(
                context = context,
                category = if (state.groupType == LibraryGroup.BY_DEFAULT) category else null,
                group = state.groupType,
                groupExtra = when (state.groupType) {
                    LibraryGroup.BY_DEFAULT -> null
                    LibraryGroup.BY_SOURCE, LibraryGroup.BY_TRACK_STATUS -> category?.id?.toString()
                    LibraryGroup.BY_STATUS -> category?.id?.minus(1)?.toString()
                    else -> null
                },
            )
            // SY <--
            scope.launch {
                val msgRes = when {
                    !started -> MR.strings.update_already_running
                    category != null -> MR.strings.updating_category
                    else -> MR.strings.updating_library
                }
                snackbarHostState.showSnackbar(context.stringResource(msgRes))
            }
            started
        }
    }

    suspend fun search(query: String) = queryEvent.send(query)

    private suspend fun requestOpenSettingsSheet() = requestSettingsSheetEvent.send(Unit)
}
