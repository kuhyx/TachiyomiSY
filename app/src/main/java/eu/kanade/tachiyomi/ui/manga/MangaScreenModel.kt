package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.Lifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.domain.chapter.interactor.GetAvailableScanlators
import eu.kanade.domain.manga.interactor.GetExcludedScanlators
import eu.kanade.domain.manga.interactor.GetPagePreviews
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.manga.model.chaptersFiltered
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import exh.eh.EHentaiUpdateHelper
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.update
import mihon.domain.source.interactor.UpdateMangaFromRemote
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.interactor.SetMangaDefaultChapterFlags
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetMangaWithChapters
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

internal class MangaScreenModel(
    internal val context: Context,
    internal val lifecycle: Lifecycle,
    internal val mangaId: Long,
    internal val isFromSource: Boolean,
    val smartSearched: Boolean,
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    trackPreferences: TrackPreferences = Injekt.get(),
    internal val readerPreferences: ReaderPreferences = Injekt.get(),
    internal val uiPreferences: UiPreferences = Injekt.get(),
    internal val downloadManager: DownloadManager = Injekt.get(),
    internal val downloadCache: DownloadCache = Injekt.get(),
    internal val getMangaAndChapters: GetMangaWithChapters = Injekt.get(),
    // SY -->
    internal val sourceManager: SourceManager = Injekt.get(),
    internal val getMergedChaptersByMangaId: GetMergedChaptersByMangaId = Injekt.get(),
    internal val getMergedMangaById: GetMergedMangaById = Injekt.get(),
    internal val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    internal val getFlatMetadata: GetFlatMetadataById = Injekt.get(),
    internal val getPagePreviews: GetPagePreviews = Injekt.get(),
    internal val setCustomMangaInfo: SetCustomMangaInfo = Injekt.get(),
    // SY <--
    internal val getAvailableScanlators: GetAvailableScanlators = Injekt.get(),
    internal val getExcludedScanlators: GetExcludedScanlators = Injekt.get(),
    internal val setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags = Injekt.get(),
    internal val updateManga: UpdateManga = Injekt.get(),
    internal val updateMangaFromRemote: UpdateMangaFromRemote = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
    // SY -->
    internal val merger: MangaMerger = MangaMerger(context),
    // SY <--
) : StateScreenModel<MangaScreenModel.State>(State.Loading) {

    internal val successState: State.Success?
        get() = state.value as? State.Success

    val manga: Manga?
        get() = successState?.manga

    val source: Source?
        get() = successState?.source

    internal val isFavorited: Boolean
        get() = manga?.favorite ?: false

    internal val allChapters: List<ChapterList.Item>?
        get() = successState?.chapters

    internal val filteredChapters: List<ChapterList.Item>?
        get() = successState?.processedChapters

    val chapterSwipeStartAction = libraryPreferences.swipeToEndAction.get()
    val chapterSwipeEndAction = libraryPreferences.swipeToStartAction.get()
    var autoTrackState = trackPreferences.autoUpdateTrackOnMarkRead.get()

    internal val skipFiltered by readerPreferences.skipFiltered.asState(screenModelScope)

    val isUpdateIntervalEnabled =
        LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD in libraryPreferences.autoUpdateMangaRestrictions.get()

    internal val selection = ChapterSelection()
    val downloads = MangaDownloads(this, context, lifecycle)
    val library = MangaLibrary(this, mangaId, libraryPreferences)
    val chapterActions = MangaChapterActions(this, context, mangaId)
    val chapterSettings = MangaChapterSettings(this, context, libraryPreferences, mangaId = mangaId)
    private val tracking = MangaTracking(this, lifecycle, mangaId)

    // EXH -->
    internal val updateHelper: EHentaiUpdateHelper by injectLazy()

    val redirectFlow: MutableSharedFlow<EXHRedirect> = MutableSharedFlow()

    data class EXHRedirect(val mangaId: Long)

    var dedupe: Boolean = true
    // EXH <--

    internal data class CombineState(
        val manga: Manga,
        val chapters: List<Chapter>,
        val flatMetadata: FlatMetadata?,
        val mergedData: MergedMangaData? = null,
        val pagePreviewsState: PagePreviewState = PagePreviewState.Loading,
    ) {
        constructor(pair: Pair<Manga, List<Chapter>>, flatMetadata: FlatMetadata?) :
            this(pair.first, pair.second, flatMetadata)
    }

    init {
        observeMangaAndChapters()
        observeExcludedScanlators()
        observeAvailableScanlators()
        downloads.observe()
        loadInitialState()
    }

    // Helper function to update the UI state only if it's currently in success state.

    /** Applies [func] to the state; the extension files reach the protected flow through it. */
    internal fun updateState(func: (MangaScreenModel.State) -> MangaScreenModel.State) {
        mutableState.update(func)
    }

    internal fun updateSuccessState(func: (State.Success) -> State.Success) {
        mutableState.update {
            when (it) {
                State.Loading -> it
                is State.Success -> func(it)
            }
        }
    }

    // SY <--

    internal fun observeTrackers() {
        tracking.observe()
    }

    sealed interface Dialog {
        data class ChangeCategory(
            val manga: Manga,
            val initialSelection: List<CheckboxState<Category>>,
        ) : Dialog
        data class DeleteChapters(val chapters: List<Chapter>) : Dialog
        data class DuplicateManga(val manga: Manga, val duplicates: List<MangaWithChapterCount>) : Dialog

        data class Migrate(val target: Manga, val current: Manga) : Dialog
        data class SetFetchInterval(val manga: Manga) : Dialog

        // SY -->
        data class EditMangaInfo(val manga: Manga) : Dialog
        data class EditMergedSettings(val mergedData: MergedMangaData) : Dialog
        // SY <--

        data object SettingsSheet : Dialog
        data object TrackSheet : Dialog
        data object FullCover : Dialog
    }

    // SY <--

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val manga: Manga,
            val source: Source,
            val isFromSource: Boolean,
            val chapters: List<ChapterList.Item>,
            val availableScanlators: Set<String>,
            val excludedScanlators: Set<String>,
            val trackingCount: Int = 0,
            val hasLoggedInTrackers: Boolean = false,
            val isRefreshingData: Boolean = false,
            val dialog: MangaScreenModel.Dialog? = null,
            val hasPromptedToAddBefore: Boolean = false,
            // SY -->
            val meta: RaisedSearchMetadata?,
            val mergedData: MergedMangaData?,
            val showRecommendationsInOverflow: Boolean,
            val showMergeInOverflow: Boolean,
            val showMergeWithAnother: Boolean,
            val pagePreviewsState: PagePreviewState,
            val alwaysShowReadingProgress: Boolean,
            val previewsRowCount: Int,
            // SY <--
        ) : State {
            val processedChapters by lazy {
                chapters.applyFilters(manga).toList()
            }

            val isAnySelected by lazy {
                chapters.fastAny { it.selected }
            }

            val chapterListItems by lazy {
                processedChapters.insertMissingCounts(manga)
            }

            val scanlatorFilterActive: Boolean
                get() = excludedScanlators.intersect(availableScanlators).isNotEmpty()

            val filterActive: Boolean
                get() = scanlatorFilterActive || manga.chaptersFiltered()
        }
    }
}
