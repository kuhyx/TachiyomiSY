package eu.kanade.tachiyomi.ui.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.library.components.LibraryContent
import eu.kanade.presentation.more.onboarding.GETTING_STARTED_URL
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen

// The sections of [LibraryTab]: toolbar, selection bottom bar, the library body, dialogs and side effects.

@Composable
internal fun LibraryTabBody(
    screenModel: LibraryScreenModel,
    state: LibraryScreenModel.State,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onClickRefresh: (Category?) -> Boolean,
) {
    when {
        state.isLoading -> {
            LoadingScreen(Modifier.padding(contentPadding))
        }
        state.searchQuery.isNullOrEmpty() && !state.hasActiveFilters && state.isLibraryEmpty -> {
            val handler = LocalUriHandler.current
            EmptyScreen(
                stringRes = MR.strings.information_empty_library,
                modifier = Modifier.padding(contentPadding),
                actions = listOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.getting_started_guide,
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = { handler.openUri(GETTING_STARTED_URL) },
                    ),
                ),
            )
        }
        else -> {
            LibraryTabList(screenModel, state, contentPadding, snackbarHostState, onClickRefresh)
        }
    }
}

@Composable
private fun LibraryTabList(
    screenModel: LibraryScreenModel,
    state: LibraryScreenModel.State,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onClickRefresh: (Category?) -> Boolean,
) {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val onContinueReading: (LibraryManga) -> Unit = { libraryManga ->
        scope.launchIO {
            val chapter = screenModel.getNextUnreadChapter(libraryManga.manga)
            if (chapter != null) {
                context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
            } else {
                snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_chapter))
            }
        }
    }
    LibraryContent(
        categories = state.displayedCategories,
        searchQuery = state.searchQuery,
        selection = state.selection,
        contentPadding = contentPadding,
        currentPage = state.coercedActiveCategoryIndex,
        hasActiveFilters = state.hasActiveFilters,
        showPageTabs = state.showCategoryTabs || !state.searchQuery.isNullOrEmpty(),
        onChangeCurrentPage = screenModel::updateActiveCategoryIndex,
        onClickManga = { navigator.push(MangaScreen(it)) },
        onContinueReadingClicked = onContinueReading.takeIf { state.showMangaContinueButton },
        onToggleSelection = screenModel::toggleSelection,
        onToggleRangeSelection = { category, manga ->
            screenModel.toggleRangeSelection(category, manga)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onRefresh = { onClickRefresh(state.activeCategory) },
        onGlobalSearchClicked = { navigator.push(GlobalSearchScreen(screenModel.state.value.searchQuery ?: "")) },
        getItemCountForCategory = { state.getItemCountForCategory(it) },
        getDisplayMode = { screenModel.getDisplayMode() },
        getColumnsForOrientation = { screenModel.getColumnsForOrientation(it) },
        getItemsForCategory = { state.getItemsForCategory(it) },
    )
}

// SY <--

// SY <--
