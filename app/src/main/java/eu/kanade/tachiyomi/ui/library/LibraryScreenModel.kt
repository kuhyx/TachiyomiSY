package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastAny
import cafe.adriel.voyager.core.model.StateScreenModel
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.presentation.library.components.LibraryToolbarTitle
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.TrackerManager
import exh.favorites.FavoritesSyncHelper
import exh.recs.batch.RecommendationSearchHelper
import exh.source.ExhPreferences
import exh.source.isEhBasedManga
import exh.source.mangaDexSourceIds
import exh.source.nHentaiSourceIds
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracksPerManga
import tachiyomi.domain.track.model.Track
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class LibraryScreenModel(
    internal val getLibraryManga: GetLibraryManga = Injekt.get(),
    internal val getCategories: GetCategories = Injekt.get(),
    internal val getTracksPerManga: GetTracksPerManga = Injekt.get(),
    internal val getNextChapters: GetNextChapters = Injekt.get(),
    internal val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    internal val setReadStatus: SetReadStatus = Injekt.get(),
    internal val updateManga: UpdateManga = Injekt.get(),
    internal val setMangaCategories: SetMangaCategories = Injekt.get(),
    internal val preferences: BasePreferences = Injekt.get(),
    internal val libraryPreferences: LibraryPreferences = Injekt.get(),
    internal val coverCache: CoverCache = Injekt.get(),
    internal val sourceManager: SourceManager = Injekt.get(),
    internal val downloadManager: DownloadManager = Injekt.get(),
    internal val downloadCache: DownloadCache = Injekt.get(),
    internal val trackerManager: TrackerManager = Injekt.get(),
    // SY -->
    internal val exhPreferences: ExhPreferences = Injekt.get(),
    internal val sourcePreferences: SourcePreferences = Injekt.get(),
    internal val getMergedMangaById: GetMergedMangaById = Injekt.get(),
    internal val setCustomMangaInfo: SetCustomMangaInfo = Injekt.get(),
    internal val getMergedChaptersByMangaId: GetMergedChaptersByMangaId = Injekt.get(),

    internal val syncPreferences: SyncPreferences = Injekt.get(),
    // SY <--
) : StateScreenModel<LibraryScreenModel.State>(State()) {

    // SY -->
    val favoritesSync = FavoritesSyncHelper(preferences.context)
    val recommendationSearch = RecommendationSearchHelper(preferences.context)

    internal var recommendationSearchJob: Job? = null
    // SY <--

    init {
        restoreActiveCategory()
        observeLibraryData()
        observeGroupedFavorites()
        observeDisplayPreferences()
        observeActiveFilters()
        // SY -->
        observeExhSync()
        observeGroupType()
        observeSyncService()
        // SY <--
    }

    internal val pipeline = LibraryItemPipeline(preferences, libraryPreferences)
    internal val search = LibrarySearch()
    internal val downloads = LibraryDownloads()
    internal val selection = LibrarySelection()

    private fun restoreActiveCategory() {
        mutableState.update { state ->
            state.copy(activeCategoryIndex = libraryPreferences.lastUsedCategory.get())
        }
    }

    /** Applies [func] to the state; the extension files reach the protected flow through it. */
    internal fun updateState(func: (State) -> State) {
        mutableState.update(func)
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
    // Unwrapped once, so the lookup compares plain longs rather than null-checking the id per category.
    val id = categoryId ?: return emptyList()
    val category = displayedCategories.find { it.id == id } ?: return emptyList()
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
