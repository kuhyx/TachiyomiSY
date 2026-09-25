package eu.kanade.tachiyomi.ui.updates

import android.content.Context
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import eu.kanade.core.preference.asState
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.updates.UpdateScreen
import eu.kanade.presentation.updates.UpdatesDeleteConfirmDialog
import eu.kanade.presentation.updates.UpdatesFilterDialog
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import mihon.feature.upcoming.UpcomingScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data object UpdatesTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 1u,
                title = stringResource(MR.strings.label_recent_updates),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(DownloadQueueScreen)
    }

    // SY -->
    @Composable
    override fun isEnabled(): Boolean {
        val scope = rememberCoroutineScope()
        return remember {
            Injekt.get<UiPreferences>().showNavUpdates.asState(scope)
        }.value
    }
    // SY <--

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { UpdatesScreenModel() }
        val settingsScreenModel = rememberScreenModel { UpdatesSettingsScreenModel() }
        val state by screenModel.state.collectAsState()

        UpdateScreen(
            state = state,
            snackbarHostState = screenModel.snackbarHostState,
            lastUpdated = screenModel.lastUpdated,
            // SY -->
            preserveReadingPosition = screenModel.preserveReadingPosition,
            // SY <--
            onClickCover = { item -> navigator.push(MangaScreen(item.update.mangaId)) },
            onSelectAll = screenModel::toggleAllSelection,
            onInvertSelection = screenModel::invertSelection,
            onUpdateLibrary = screenModel::updateLibrary,
            onDownloadChapter = screenModel::downloadChapters,
            onMultiBookmarkClicked = screenModel::bookmarkUpdates,
            onMultiMarkAsReadClicked = screenModel::markUpdatesRead,
            onMultiDeleteClicked = screenModel::showConfirmDeleteChapters,
            onUpdateSelected = screenModel::toggleSelection,
            onOpenChapter = {
                val intent = ReaderActivity.newIntent(context, it.update.mangaId, it.update.chapterId)
                context.startActivity(intent)
            },
            onCalendarClicked = { navigator.push(UpcomingScreen()) },
            onFilterClicked = screenModel::showFilterDialog,
            hasActiveFilters = state.hasActiveFilters,
        )

        UpdatesDialog(screenModel, settingsScreenModel, state.dialog)
        LaunchedEffect(Unit) { showEvents(screenModel, context) }

        LaunchedEffect(state.selectionMode) {
            HomeScreen.showBottomNav(!state.selectionMode)
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }
        DisposableEffect(Unit) {
            screenModel.resetNewUpdatesCount()

            onDispose {
                screenModel.resetNewUpdatesCount()
            }
        }
    }

    @Composable
    private fun UpdatesDialog(
        screenModel: UpdatesScreenModel,
        settingsScreenModel: UpdatesSettingsScreenModel,
        dialog: UpdatesScreenModel.Dialog?,
    ) {
        val onDismissDialog = { screenModel.setDialog(null) }
        when (dialog) {
            is UpdatesScreenModel.Dialog.DeleteConfirmation -> {
                UpdatesDeleteConfirmDialog(
                    onDismissRequest = onDismissDialog,
                    onConfirm = { screenModel.deleteChapters(dialog.toDelete) },
                )
            }
            is UpdatesScreenModel.Dialog.FilterSheet -> {
                UpdatesFilterDialog(
                    onDismissRequest = onDismissDialog,
                    screenModel = settingsScreenModel,
                )
            }
            // No dialog is the `else`: a `when` without one gets a dead "no match" group from Compose.
            else -> {}
        }
    }

    // A plain function rather than a composable, so no Compose memoisation guards: mapLatest/launchIn because a
    // newer event still replaces the snackbar and the never-ending channel leaves nothing to run after a collect.
    private fun CoroutineScope.showEvents(screenModel: UpdatesScreenModel, context: Context) {
        screenModel.events
            .mapLatest { event -> screenModel.snackbarHostState.showSnackbar(context.stringResource(event.message)) }
            .launchIn(this)
    }
}
