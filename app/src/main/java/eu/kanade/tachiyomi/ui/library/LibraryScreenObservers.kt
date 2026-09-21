package eu.kanade.tachiyomi.ui.library

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.LibraryData
import exh.source.EH_SOURCE_ID
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.i18n.MR
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

internal fun LibraryScreenModel.observeLibraryData() {
    screenModelScope.launchIO {
        combine(
            combine(
                state.map { it.searchQuery }.distinctUntilChanged().debounce(0.25.seconds),
                getCategories.subscribe(),
                getFavoritesFlow(),
                ::Triple,
            ),
            combine(
                getTracksPerManga.subscribe(),
                getTrackingFiltersFlow(),
                ::Pair,
            ),
            // SY -->
            combine(
                state.map { it.groupType }.distinctUntilChanged(),
                libraryPreferences.sortingMode.changes(),
                ::Pair,
            ),
            // SY <--
            getLibraryItemPreferencesFlow(),
        ) {
                (searchQuery, categories, favorites),
                (tracksMap, trackingFilters),
                // SY -->
                (groupType, sortingMode),
                // SY <--
                itemPreferences,
            ->
            val showSystemCategory = favorites.any { it.libraryManga.categories.contains(0) }
            val filteredFavorites = with(pipeline) {
                favorites.applyFilters(tracksMap, trackingFilters, itemPreferences)
            }
                .let {
                    if (searchQuery == null) {
                        it
                    } else {
                        // SY -->
                        // it.filter { m -> m.matches(searchQuery) } }
                        search.filterLibrary(it, searchQuery, trackingFilters)
                        // SY <--
                    }
                }

            LibraryData(
                isInitialized = true,
                showSystemCategory = showSystemCategory,
                categories = categories,
                favorites = filteredFavorites,
                tracksMap = tracksMap,
                loggedInTrackerIds = trackingFilters.keys,
            )
        }
            .distinctUntilChanged()
            .collectLatest { libraryData ->
                updateState { state ->
                    state.copy(libraryData = libraryData)
                }
            }
    }
}

internal fun LibraryScreenModel.observeGroupedFavorites() {
    screenModelScope.launchIO {
        state
            .dropWhile { !it.libraryData.isInitialized }
            .map {
                Pair(
                    it.libraryData,
                    // SY -->
                    it.groupType,
                    // SY <--
                )
            }
            .distinctUntilChanged()
            .map { (data, groupType) ->
                with(pipeline) {
                    data.favorites
                        .applyGrouping(
                            data.categories,
                            data.showSystemCategory,
                            // SY -->
                            groupType,
                            // SY <--
                        )
                        .applySort(
                            data.favoritesById,
                            data.tracksMap,
                            data.loggedInTrackerIds,
                            // SY -->
                            libraryPreferences.sortingMode.get().takeIf { groupType != LibraryGroup.BY_DEFAULT },
                            // SY <--
                        )
                }
                    .let {
                        it.ifEmpty {
                            mapOf(
                                Category(
                                    0,
                                    preferences.context.stringResource(MR.strings.default_category),
                                    0,
                                    0,
                                ) to emptyList(),
                            )
                        }
                    }
            }
            .collectLatest {
                updateState { state ->
                    state.copy(
                        isLoading = false,
                        groupedFavorites = it,
                    )
                }
            }
    }
}

internal fun LibraryScreenModel.observeDisplayPreferences() {
    combine(
        libraryPreferences.categoryTabs.changes(),
        libraryPreferences.categoryNumberOfItems.changes(),
        libraryPreferences.showContinueReadingButton.changes(),
    ) { a, b, c -> arrayOf(a, b, c) }
        .onEach { (showCategoryTabs, showMangaCount, showMangaContinueButton) ->
            updateState { state ->
                state.copy(
                    showCategoryTabs = showCategoryTabs,
                    showMangaCount = showMangaCount,
                    showMangaContinueButton = showMangaContinueButton,
                )
            }
        }
        .launchIn(screenModelScope)
}

internal fun LibraryScreenModel.observeActiveFilters() {
    combine(
        getLibraryItemPreferencesFlow(),
        getTrackingFiltersFlow(),
    ) { prefs, trackFilters ->
        val filters = listOf(
            prefs.filterDownloaded,
            prefs.filterUnread,
            prefs.filterStarted,
            prefs.filterBookmarked,
            prefs.filterCompleted,
            prefs.filterIntervalCustom,
            // SY -->
            prefs.filterLewd,
            // SY <--
        ) + trackFilters.values
        filters.any { it != TriState.DISABLED }
    }
        .distinctUntilChanged()
        .onEach {
            updateState { state ->
                state.copy(hasActiveFilters = it)
            }
        }
        .launchIn(screenModelScope)
}

// SY -->

internal fun LibraryScreenModel.observeExhSync() {
    combine(
        exhPreferences.isHentaiEnabled.changes(),
        sourcePreferences.disabledSources.changes(),
        exhPreferences.enableExhentai.changes(),
    ) { isHentaiEnabled, disabledSources, enableExhentai ->
        isHentaiEnabled && (EH_SOURCE_ID.toString() !in disabledSources || enableExhentai)
    }
        .distinctUntilChanged()
        .onEach {
            updateState { state ->
                state.copy(showSyncExh = it)
            }
        }
        .launchIn(screenModelScope)
}

internal fun LibraryScreenModel.observeGroupType() {
    libraryPreferences.groupLibraryBy.changes()
        .onEach {
            updateState { state ->
                state.copy(groupType = it)
            }
        }
        .launchIn(screenModelScope)
}

internal fun LibraryScreenModel.observeSyncService() {
    syncPreferences.syncService
        .changes()
        .distinctUntilChanged()
        .onEach { syncService ->
            updateState { it.copy(isSyncEnabled = syncService != 0) }
        }
        .launchIn(screenModelScope)
}

// SY <--
