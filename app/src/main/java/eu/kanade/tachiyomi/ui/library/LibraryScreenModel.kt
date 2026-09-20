package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.core.preference.asState
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.presentation.library.components.LibraryToolbarTitle
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.util.chapter.getNextUnread
import eu.kanade.tachiyomi.util.removeCovers
import exh.favorites.FavoritesSyncHelper
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import exh.recs.batch.RecommendationSearchHelper
import exh.source.EH_SOURCE_ID
import exh.source.ExhPreferences
import exh.source.MERGED_SOURCE_ID
import exh.source.isEhBasedManga
import exh.source.mangaDexSourceIds
import exh.source.nHentaiSourceIds
import exh.util.nullIfBlank
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracksPerManga
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

internal class LibraryScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val getTracksPerManga: GetTracksPerManga = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val setReadStatus: SetReadStatus = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val preferences: BasePreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadCache: DownloadCache = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    // SY -->
    private val exhPreferences: ExhPreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val getMergedMangaById: GetMergedMangaById = Injekt.get(),
    private val setCustomMangaInfo: SetCustomMangaInfo = Injekt.get(),
    private val getMergedChaptersByMangaId: GetMergedChaptersByMangaId = Injekt.get(),

    syncPreferences: SyncPreferences = Injekt.get(),
    // SY <--
) : StateScreenModel<LibraryScreenModel.State>(State()) {

    // SY -->
    val favoritesSync = FavoritesSyncHelper(preferences.context)
    val recommendationSearch = RecommendationSearchHelper(preferences.context)

    private var recommendationSearchJob: Job? = null
    // SY <--

    init {
        mutableState.update { state ->
            state.copy(activeCategoryIndex = libraryPreferences.lastUsedCategory.get())
        }
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
                    mutableState.update { state ->
                        state.copy(libraryData = libraryData)
                    }
                }
        }

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
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            groupedFavorites = it,
                        )
                    }
                }
        }

        combine(
            libraryPreferences.categoryTabs.changes(),
            libraryPreferences.categoryNumberOfItems.changes(),
            libraryPreferences.showContinueReadingButton.changes(),
        ) { a, b, c -> arrayOf(a, b, c) }
            .onEach { (showCategoryTabs, showMangaCount, showMangaContinueButton) ->
                mutableState.update { state ->
                    state.copy(
                        showCategoryTabs = showCategoryTabs,
                        showMangaCount = showMangaCount,
                        showMangaContinueButton = showMangaContinueButton,
                    )
                }
            }
            .launchIn(screenModelScope)

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
                mutableState.update { state ->
                    state.copy(hasActiveFilters = it)
                }
            }
            .launchIn(screenModelScope)

        // SY -->
        combine(
            exhPreferences.isHentaiEnabled.changes(),
            sourcePreferences.disabledSources.changes(),
            exhPreferences.enableExhentai.changes(),
        ) { isHentaiEnabled, disabledSources, enableExhentai ->
            isHentaiEnabled && (EH_SOURCE_ID.toString() !in disabledSources || enableExhentai)
        }
            .distinctUntilChanged()
            .onEach {
                mutableState.update { state ->
                    state.copy(showSyncExh = it)
                }
            }
            .launchIn(screenModelScope)

        libraryPreferences.groupLibraryBy.changes()
            .onEach {
                mutableState.update { state ->
                    state.copy(groupType = it)
                }
            }
            .launchIn(screenModelScope)
        syncPreferences.syncService
            .changes()
            .distinctUntilChanged()
            .onEach { syncService ->
                mutableState.update { it.copy(isSyncEnabled = syncService != 0) }
            }
            .launchIn(screenModelScope)
        // SY <--
    }

    private val pipeline = LibraryItemPipeline(preferences, libraryPreferences)
    private val search = LibrarySearch()
    private val downloads = LibraryDownloads()
    private val selection = LibrarySelection()

    private fun getLibraryItemPreferencesFlow(): Flow<ItemPreferences> {
        return combine(
            libraryPreferences.downloadBadge.changes(),
            libraryPreferences.unreadBadge.changes(),
            libraryPreferences.localBadge.changes(),
            libraryPreferences.languageBadge.changes(),
            libraryPreferences.autoUpdateMangaRestrictions.changes(),

            preferences.downloadedOnly.changes(),
            libraryPreferences.filterDownloaded.changes(),
            libraryPreferences.filterUnread.changes(),
            libraryPreferences.filterStarted.changes(),
            libraryPreferences.filterBookmarked.changes(),
            libraryPreferences.filterCompleted.changes(),
            libraryPreferences.filterIntervalCustom.changes(),
            // SY -->
            libraryPreferences.filterLewd.changes(),
            // SY <--
        ) {
            ItemPreferences(
                downloadBadge = it[0] as Boolean,
                unreadBadge = it[1] as Boolean,
                localBadge = it[2] as Boolean,
                languageBadge = it[3] as Boolean,
                skipOutsideReleasePeriod = LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD in it[4] as Set<*>,
                globalFilterDownloaded = it[5] as Boolean,
                filterDownloaded = it[6] as TriState,
                filterUnread = it[7] as TriState,
                filterStarted = it[8] as TriState,
                filterBookmarked = it[9] as TriState,
                filterCompleted = it[10] as TriState,
                filterIntervalCustom = it[11] as TriState,
                // SY -->
                filterLewd = it[12] as TriState,
                // SY <--
            )
        }
    }

    private fun getFavoritesFlow(): Flow<List<LibraryItem>> {
        return combine(
            getLibraryManga.subscribe(),
            getLibraryItemPreferencesFlow(),
            downloadCache.changes,
        ) { libraryManga, preferences, _ ->
            libraryManga.map { manga ->
                // SY -->
                val downloadCount = if (manga.manga.source == MERGED_SOURCE_ID) {
                    getMergedMangaById.await(manga.manga.id).sumOf { downloadManager.getDownloadCount(it) }
                } else {
                    downloadManager.getDownloadCount(manga.manga)
                }
                // SY <--
                LibraryItem(
                    libraryManga = manga,
                    // SY -->
                    downloadCount = downloadCount,
                    // SY <--
                    unreadCount = manga.unreadCount,
                    isLocal = manga.manga.isLocal(),
                    badges = LibraryItem.Badges(
                        downloadCount = if (preferences.downloadBadge) {
                            // SY -->
                            downloadCount
                            // SY <--
                        } else {
                            0
                        },
                        unreadCount = if (preferences.unreadBadge) {
                            manga.unreadCount
                        } else {
                            0
                        },
                        isLocal = if (preferences.localBadge) {
                            manga.manga.isLocal()
                        } else {
                            false
                        },
                        sourceLanguage = if (preferences.languageBadge) {
                            sourceManager.getOrStub(manga.manga.source).lang
                        } else {
                            ""
                        },
                    ),
                )
            }
        }
    }

    // Flow of tracking filter preferences.
    // @return map of track id with the filter value
    private fun getTrackingFiltersFlow(): Flow<Map<Long, TriState>> {
        return trackerManager.loggedInTrackersFlow().flatMapLatest { loggedInTrackers ->
            if (loggedInTrackers.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val filterFlows = loggedInTrackers.map { tracker ->
                    libraryPreferences.filterTracking(tracker.id.toInt()).changes().map { tracker.id to it }
                }
                combine(filterFlows) { it.toMap() }
            }
        }
    }

    // Returns the common categories for the given list of manga.
    // @param mangas the list of manga.
    private suspend fun getCommonCategories(mangas: List<Manga>): Collection<Category> {
        if (mangas.isEmpty()) return emptyList()
        return mangas
            .map { getCategories.await(it.id).toSet() }
            .reduce { set1, set2 -> set1.intersect(set2) }
    }

    suspend fun getNextUnreadChapter(manga: Manga): Chapter? {
        // SY -->
        val mergedManga = getMergedMangaById.await(manga.id).associateBy { it.id }
        return if (manga.id == MERGED_SOURCE_ID) {
            getMergedChaptersByMangaId.await(manga.id, applyScanlatorFilter = true)
        } else {
            getChaptersByMangaId.await(manga.id, applyScanlatorFilter = true)
        }.getNextUnread(manga, downloadManager, mergedManga)
        // SY <--
    }

    // Returns the mix (non-common) categories for the given list of manga.
    // @param mangas the list of manga.
    private suspend fun getMixCategories(mangas: List<Manga>): Collection<Category> {
        if (mangas.isEmpty()) return emptyList()
        val mangaCategories = mangas.map { getCategories.await(it.id).toSet() }
        val common = mangaCategories.reduce { set1, set2 -> set1.intersect(set2) }
        return mangaCategories.flatten().distinct().subtract(common)
    }

    /**
     * Queues the amount specified of unread chapters from the list of selected manga.
     */
    fun performDownloadAction(action: DownloadAction) {
        val mangas = state.value.selectedManga
        screenModelScope.launchNonCancellable {
            if (action == DownloadAction.BOOKMARKED_CHAPTERS) {
                downloads.downloadBookmarkedChapters(mangas)
            } else {
                downloads.downloadNextChapters(mangas, action.nextChapters)
            }
        }
        clearSelection()
    }

    // SY -->
    fun cleanTitles() {
        state.value.selectedManga.fastFilter {
            it.isEhBasedManga() ||
                it.source in nHentaiSourceIds
        }.fastForEach { manga ->
            val editedTitle =
                manga.title.replace("\\[.*?]".toRegex(), "").trim().replace("\\(.*?\\)".toRegex(), "").trim()
                    .replace("\\{.*?\\}".toRegex(), "")
                    .trim()
                    .let {
                        if (it.contains("|")) {
                            it.replace(".*\\|".toRegex(), "").trim()
                        } else {
                            it
                        }
                    }
            if (manga.title == editedTitle) return@fastForEach
            val mangaInfo = CustomMangaInfo(
                id = manga.id,
                title = editedTitle.nullIfBlank(),
                author = manga.author.takeUnless { it == manga.ogAuthor },
                artist = manga.artist.takeUnless { it == manga.ogArtist },
                thumbnailUrl = manga.thumbnailUrl.takeUnless { it == manga.ogThumbnailUrl },
                description = manga.description.takeUnless { it == manga.ogDescription },
                genre = manga.genre.takeUnless { it == manga.ogGenre },
                status = manga.status.takeUnless { it == manga.ogStatus },
            )

            setCustomMangaInfo.set(mangaInfo)
        }
        clearSelection()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun syncMangaToDex() {
        launchIO {
            MdUtil.getEnabledMangaDex(sourcePreferences, sourceManager)?.let { mdex ->
                state.value.selectedManga.fastFilter { it.source in mangaDexSourceIds }.fastForEach { manga ->
                    mdex.updateFollowStatus(MdUtil.getMangaId(manga.url), FollowStatus.READING)
                }
            }
            clearSelection()
        }
    }

    fun resetInfo() {
        state.value.selectedManga.fastForEach { manga ->
            val mangaInfo = CustomMangaInfo(
                id = manga.id,
                title = null,
                author = null,
                artist = null,
                thumbnailUrl = null,
                description = null,
                genre = null,
                status = null,
            )

            setCustomMangaInfo.set(mangaInfo)
        }
        clearSelection()
    }
// SY <--

    /**
     * Marks mangas' chapters read status.
     */
    fun markReadSelection(read: Boolean) {
        val selection = state.value.selectedManga
        screenModelScope.launchNonCancellable {
            selection.forEach { manga ->
                setReadStatus.await(
                    manga = manga,
                    read = read,
                )
            }
        }
        clearSelection()
    }

    /**
     * Remove the selected manga.
     *
     * @param mangas the list of manga to delete.
     * @param deleteFromLibrary whether to delete manga from library.
     * @param deleteChapters whether to delete downloaded chapters.
     */
    fun removeMangas(mangas: List<Manga>, deleteFromLibrary: Boolean, deleteChapters: Boolean) {
        screenModelScope.launchNonCancellable {
            if (deleteFromLibrary) {
                val toDelete = mangas.map {
                    it.removeCovers(coverCache)
                    MangaUpdate(
                        favorite = false,
                        id = it.id,
                    )
                }
                updateManga.awaitAll(toDelete)
            }

            if (deleteChapters) {
                mangas.forEach { manga ->
                    val source = sourceManager.get(manga.source) as? HttpSource
                    if (source != null) {
                        if (source is MergedSource) {
                            val mergedMangas = getMergedMangaById.await(manga.id)
                            val sources = mergedMangas.distinctBy {
                                it.source
                            }.map { sourceManager.getOrStub(it.source) }
                            mergedMangas.forEach merge@{ mergedManga ->
                                val mergedSource =
                                    sources.firstOrNull { mergedManga.source == it.id } as? HttpSource ?: return@merge
                                downloadManager.deleteManga(mergedManga, mergedSource)
                            }
                        } else {
                            downloadManager.deleteManga(manga, source)
                        }
                    }
                }
            }
        }
    }

    /**
     * Bulk update categories of manga using old and new common categories.
     *
     * @param mangaList the list of manga to move.
     * @param addCategories the categories to add for all mangas.
     * @param removeCategories the categories to remove in all mangas.
     */
    fun setMangaCategories(mangaList: List<Manga>, addCategories: List<Long>, removeCategories: List<Long>) {
        screenModelScope.launchNonCancellable {
            mangaList.forEach { manga ->
                val categoryIds = getCategories.await(manga.id)
                    .map { it.id }
                    .subtract(removeCategories.toSet())
                    .plus(addCategories)
                    .toList()

                setMangaCategories.await(manga.id, categoryIds)
            }
        }
    }

    fun getDisplayMode(): PreferenceMutableState<LibraryDisplayMode> =
        libraryPreferences.displayMode.asState(screenModelScope)

    fun getColumnsForOrientation(isLandscape: Boolean): PreferenceMutableState<Int> {
        return (if (isLandscape) libraryPreferences.landscapeColumns else libraryPreferences.portraitColumns)
            .asState(screenModelScope)
    }

    fun randomItemInCurrentCategory(): LibraryItem? {
        val state = state.value
        return state.getItemsForCategoryId(state.activeCategory?.id).randomOrNull()
    }

    fun showSettingsDialog() {
        mutableState.update { it.copy(dialog = Dialog.SettingsSheet) }
    }

    // SY -->
    fun showRecommendationSearchDialog() {
        val mangaList = state.value.selectedManga
        mutableState.update { it.copy(dialog = Dialog.RecommendationSearchSheet(mangaList)) }
    }

    fun clearSelection() {
        mutableState.update { selection.clear(it) }
    }

    fun toggleSelection(category: Category, manga: LibraryManga) {
        mutableState.update { selection.toggle(it, category, manga) }
    }

    fun toggleRangeSelection(category: Category, manga: LibraryManga) {
        mutableState.update { selection.toggleRange(it, category, manga) }
    }

    fun selectAll() {
        mutableState.update { selection.selectAll(it) }
    }

    fun invertSelection() {
        mutableState.update { selection.invert(it) }
    }

    fun search(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun updateActiveCategoryIndex(index: Int) {
        val newIndex = mutableState.updateAndGet { state ->
            state.copy(activeCategoryIndex = index)
        }
            .coercedActiveCategoryIndex

        libraryPreferences.lastUsedCategory.set(newIndex)
    }

    fun openChangeCategoryDialog() {
        screenModelScope.launchIO {
            // Create a copy of selected manga
            val mangaList = state.value.selectedManga

            // Hide the default category because it has a different behavior than the ones from db.
            // SY -->
            val categories = state.value.libraryData.categories.filter { it.id != 0L }
            // SY <--

            // Get indexes of the common categories to preselect.
            val common = getCommonCategories(mangaList)
            // Get indexes of the mix categories to preselect.
            val mix = getMixCategories(mangaList)
            val preselected = categories
                .map {
                    when (it) {
                        in common -> CheckboxState.State.Checked(it)
                        in mix -> CheckboxState.TriState.Exclude(it)
                        else -> CheckboxState.State.None(it)
                    }
                }

            mutableState.update { it.copy(dialog = Dialog.ChangeCategory(mangaList, preselected)) }
        }
    }

    fun openDeleteMangaDialog() {
        mutableState.update { it.copy(dialog = Dialog.DeleteManga(state.value.selectedManga)) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    sealed interface Dialog {
        data object SettingsSheet : Dialog
        data class ChangeCategory(
            val manga: List<Manga>,
            val initialSelection: List<CheckboxState<Category>>,
        ) : Dialog

        data class DeleteManga(val manga: List<Manga>) : Dialog

        // SY -->
        data object SyncFavoritesWarning : Dialog
        data object SyncFavoritesConfirm : Dialog
        data class RecommendationSearchSheet(val manga: List<Manga>) : Dialog
        // SY <--
    }

// SY -->

    /** Returns first unread chapter of a manga. */
    suspend fun getFirstUnread(manga: Manga): Chapter? = getNextChapters.await(manga.id).firstOrNull()

    fun runRecommendationSearch(selection: List<Manga>) {
        recommendationSearch.runSearch(screenModelScope, selection)?.let {
            recommendationSearchJob = it
        }
    }

    fun cancelRecommendationSearch() {
        recommendationSearchJob?.cancel()
    }

    fun runSync() {
        favoritesSync.runSync(screenModelScope)
    }

    fun onAcceptSyncWarning() {
        exhPreferences.exhShowSyncIntro.set(false)
    }

    fun openFavoritesSyncDialog() {
        mutableState.update {
            it.copy(
                dialog = if (exhPreferences.exhShowSyncIntro.get()) {
                    Dialog.SyncFavoritesWarning
                } else {
                    Dialog.SyncFavoritesConfirm
                },
            )
        }
    }
// SY <--

    @Immutable
    internal data class ItemPreferences(
        val downloadBadge: Boolean,
        val unreadBadge: Boolean,
        val localBadge: Boolean,
        val languageBadge: Boolean,
        val skipOutsideReleasePeriod: Boolean,

        val globalFilterDownloaded: Boolean,
        val filterDownloaded: TriState,
        val filterUnread: TriState,
        val filterStarted: TriState,
        val filterBookmarked: TriState,
        val filterCompleted: TriState,
        val filterIntervalCustom: TriState,
        // SY -->
        val filterLewd: TriState,
        // SY <--
    )

    @Immutable
    data class LibraryData(
        val isInitialized: Boolean = false,
        val showSystemCategory: Boolean = false,
        val categories: List<Category> = emptyList(),
        val favorites: List<LibraryItem> = emptyList(),
        val tracksMap: Map</* Manga */ Long, List<Track>> = emptyMap(),
        val loggedInTrackerIds: Set<Long> = emptySet(),
    ) {
        val favoritesById by lazy { favorites.associateBy { it.id } }
    }

    @Immutable
    data class State(
        val isInitialized: Boolean = false,
        val isLoading: Boolean = true,
        val searchQuery: String? = null,
        val selection: Set</* Manga */ Long> = setOf(),
        val hasActiveFilters: Boolean = false,
        val showCategoryTabs: Boolean = false,
        val showMangaCount: Boolean = false,
        val showMangaContinueButton: Boolean = false,
        val dialog: Dialog? = null,
        val libraryData: LibraryData = LibraryData(),
        private val activeCategoryIndex: Int = 0,
        internal val groupedFavorites: Map<Category, List</* LibraryItem */ Long>> = emptyMap(),
        // SY -->
        val showSyncExh: Boolean = false,
        val isSyncEnabled: Boolean = false,
        val groupType: Int = LibraryGroup.BY_DEFAULT,
        // SY <--
    ) {
        val displayedCategories: List<Category> = groupedFavorites.keys.toList()

        val coercedActiveCategoryIndex = activeCategoryIndex.coerceIn(
            minimumValue = 0,
            maximumValue = displayedCategories.lastIndex.coerceAtLeast(0),
        )

        val activeCategory: Category? = displayedCategories.getOrNull(coercedActiveCategoryIndex)

        val isLibraryEmpty = libraryData.favorites.isEmpty()

        val selectionMode = selection.isNotEmpty()

        val selectedManga by lazy { selection.mapNotNull { libraryData.favoritesById[it]?.libraryManga?.manga } }

        // SY -->
        val showCleanTitles: Boolean by lazy {
            selectedManga.fastAny {
                it.isEhBasedManga() ||
                    it.source in nHentaiSourceIds
            }
        }

        val showAddToMangadex: Boolean by lazy {
            selectedManga.any { it.source in mangaDexSourceIds }
        }

        val showResetInfo: Boolean by lazy {
            selectedManga.fastAny { manga ->
                manga.title != manga.ogTitle ||
                    manga.author != manga.ogAuthor ||
                    manga.artist != manga.ogArtist ||
                    manga.thumbnailUrl != manga.ogThumbnailUrl ||
                    manga.description != manga.ogDescription ||
                    manga.genre != manga.ogGenre ||
                    manga.status != manga.ogStatus
            }
        }
        // SY <--
    }
}

internal fun LibraryScreenModel.State.getItemsForCategoryId(categoryId: Long?): List<LibraryItem> {
    if (categoryId == null) return emptyList()
    val category = displayedCategories.find { it.id == categoryId } ?: return emptyList()
    return getItemsForCategory(category)
}

internal fun LibraryScreenModel.State.getItemsForCategory(category: Category): List<LibraryItem> =
    groupedFavorites[category].orEmpty().mapNotNull { libraryData.favoritesById[it] }

internal fun LibraryScreenModel.State.getItemCountForCategory(category: Category): Int? =
    if (showMangaCount || !searchQuery.isNullOrEmpty()) groupedFavorites[category]?.size else null

internal fun LibraryScreenModel.State.getToolbarTitle(
    defaultTitle: String,
    defaultCategoryTitle: String,
    page: Int,
): LibraryToolbarTitle {
    val category = displayedCategories.getOrNull(page) ?: return LibraryToolbarTitle(defaultTitle)
    val categoryName = category.let {
        if (it.isSystemCategory) defaultCategoryTitle else it.name
    }
    val title = if (showCategoryTabs) defaultTitle else categoryName
    val count = when {
        !showMangaCount -> null
        !showCategoryTabs -> getItemCountForCategory(category)
        // Whole library count
        else -> libraryData.favorites.size
    }
    return LibraryToolbarTitle(title, count)
}
