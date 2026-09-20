package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.chapter.interactor.GetAvailableScanlators
import eu.kanade.domain.manga.interactor.GetExcludedScanlators
import eu.kanade.domain.manga.interactor.GetPagePreviews
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.domain.manga.model.chaptersFiltered
import eu.kanade.domain.manga.model.downloadedFilter
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import exh.debug.DebugToggles
import exh.eh.EHentaiUpdateHelper
import exh.log.xLogD
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.metadata.base.raise
import exh.source.MERGED_SOURCE_ID
import exh.source.getMainSource
import exh.source.isEhBasedManga
import exh.util.nullIfEmpty
import exh.util.trimOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.domain.source.interactor.UpdateMangaFromRemote
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.interactor.SetMangaDefaultChapterFlags
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.chapter.service.calculateChapterGap
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetMangaWithChapters
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.manga.model.applyFilter
import tachiyomi.domain.manga.model.bookmarkedFilter
import tachiyomi.domain.manga.model.sortDescending
import tachiyomi.domain.manga.model.unreadFilter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.floor

internal class MangaScreenModel(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val mangaId: Long,
    private val isFromSource: Boolean,
    val smartSearched: Boolean,
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    trackPreferences: TrackPreferences = Injekt.get(),
    readerPreferences: ReaderPreferences = Injekt.get(),
    uiPreferences: UiPreferences = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadCache: DownloadCache = Injekt.get(),
    private val getMangaAndChapters: GetMangaWithChapters = Injekt.get(),
    // SY -->
    private val sourceManager: SourceManager = Injekt.get(),
    private val getMergedChaptersByMangaId: GetMergedChaptersByMangaId = Injekt.get(),
    private val getMergedMangaById: GetMergedMangaById = Injekt.get(),
    private val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    private val getFlatMetadata: GetFlatMetadataById = Injekt.get(),
    private val getPagePreviews: GetPagePreviews = Injekt.get(),
    private val setCustomMangaInfo: SetCustomMangaInfo = Injekt.get(),
    // SY <--
    private val getAvailableScanlators: GetAvailableScanlators = Injekt.get(),
    private val getExcludedScanlators: GetExcludedScanlators = Injekt.get(),
    private val setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val updateMangaFromRemote: UpdateMangaFromRemote = Injekt.get(),
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
    private val updateHelper: EHentaiUpdateHelper by injectLazy()

    val redirectFlow: MutableSharedFlow<EXHRedirect> = MutableSharedFlow()

    data class EXHRedirect(val mangaId: Long)

    var dedupe: Boolean = true
    // EXH <--

    private data class CombineState(
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
        screenModelScope.launchIO {
            getMangaAndChapters.subscribe(mangaId, applyScanlatorFilter = true)
                .distinctUntilChanged()
                // SY -->
                .combine(
                    getMergedChaptersByMangaId.subscribe(mangaId, true, applyScanlatorFilter = true)
                        .distinctUntilChanged(),
                ) { (manga, chapters), mergedChapters ->
                    if (manga.source == MERGED_SOURCE_ID) {
                        manga to mergedChapters
                    } else {
                        manga to chapters
                    }
                }
                .onEach { (manga, chapters) ->
                    if (chapters.isNotEmpty() &&
                        manga.isEhBasedManga() &&
                        DebugToggles.ENABLE_EXH_ROOT_REDIRECT.enabled
                    ) {
                        // Check for gallery in library and accept manga with lowest id
                        // Find chapters sharing same root
                        launchIO {
                            try {
                                val (acceptedChain) =
                                    updateHelper.acceptRootAndDiscardOthers(manga.source, chapters)
                                // Redirect if we are not the accepted root
                                if (manga.id != acceptedChain.manga.id && acceptedChain.manga.favorite) {
                                    // Update if any of our chapters are not in accepted manga's chapters
                                    xLogD("Found accepted manga %s", manga.url)
                                    redirectFlow.emit(
                                        EXHRedirect(acceptedChain.manga.id),
                                    )
                                }
                            } catch (expected: Exception) {
                                // Logged whatever the cause; the caller carries on.
                                logcat(LogPriority.ERROR, expected) { "Error loading accepted chapter chain" }
                            }
                        }
                    }
                }
                .combine(
                    getFlatMetadata.subscribe(mangaId)
                        .distinctUntilChanged(),
                ) { pair, flatMetadata ->
                    CombineState(pair, flatMetadata)
                }
                .combine(
                    combine(
                        getMergedMangaById.subscribe(mangaId)
                            .distinctUntilChanged(),
                        getMergedReferencesById.subscribe(mangaId)
                            .distinctUntilChanged(),
                    ) { manga, references ->
                        if (manga.isNotEmpty()) {
                            MergedMangaData(
                                references,
                                manga.associateBy { it.id },
                                references.map { it.mangaSourceId }.distinct()
                                    .map { sourceManager.getOrStub(it) },
                            )
                        } else {
                            null
                        }
                    },
                ) { state, mergedData ->
                    state.copy(mergedData = mergedData)
                }
                .combine(downloadCache.changes) { state, _ -> state }
                .combine(downloadManager.queueState) { state, _ -> state }
                // SY <--
                .flowWithLifecycle(lifecycle)
                .collectLatest { combined ->
                    val manga = combined.manga
                    val mergedData = combined.mergedData
                    val chapterItems = combined.chapters.toChapterListItems(manga /* SY --> */, mergedData /* SY <-- */)
                    updateSuccessState {
                        it.copy(
                            manga = manga,
                            chapters = chapterItems,
                            // SY -->
                            meta = raiseMetadata(combined.flatMetadata, it.source),
                            mergedData = mergedData,
                            // SY <--
                        )
                    }
                }
        }

        screenModelScope.launchIO {
            getExcludedScanlators.subscribe(mangaId)
                .flowWithLifecycle(lifecycle)
                .distinctUntilChanged()
                .collectLatest { excludedScanlators ->
                    updateSuccessState {
                        it.copy(excludedScanlators = excludedScanlators)
                    }
                }
        }

        screenModelScope.launchIO {
            getAvailableScanlators.subscribe(mangaId)
                .flowWithLifecycle(lifecycle)
                .distinctUntilChanged()
                // SY -->
                .combine(
                    state.map { (it as? State.Success)?.manga }
                        .distinctUntilChangedBy { it?.source }
                        .flatMapConcat {
                            if (it?.source == MERGED_SOURCE_ID) {
                                getAvailableScanlators.subscribeMerge(mangaId)
                            } else {
                                flowOf(emptySet())
                            }
                        },
                ) { mangaScanlators, mergeScanlators ->
                    mangaScanlators + mergeScanlators
                } // SY <--
                .collectLatest { availableScanlators ->
                    updateSuccessState {
                        it.copy(availableScanlators = availableScanlators)
                    }
                }
        }

        downloads.observe()

        screenModelScope.launchIO {
            val manga = getMangaAndChapters.awaitManga(mangaId)
            // SY -->
            val mergedData = getMergedReferencesById.await(mangaId).takeIf { it.isNotEmpty() }?.let { references ->
                MergedMangaData(
                    references,
                    getMergedMangaById.await(mangaId).associateBy { it.id },
                    references.map { it.mangaSourceId }.distinct()
                        .map { sourceManager.getOrStub(it) },
                )
            }
            val rawChapters = if (manga.source == MERGED_SOURCE_ID) {
                getMergedChaptersByMangaId.await(mangaId, applyScanlatorFilter = true)
            } else {
                getMangaAndChapters.awaitChapters(mangaId, applyScanlatorFilter = true)
            }
            val chapters = rawChapters.toChapterListItems(manga, mergedData)
            val meta = getFlatMetadata.await(mangaId)
            // SY <--

            if (!manga.favorite) {
                setMangaDefaultChapterFlags.await(manga)
            }

            val needRefreshInfo = !manga.initialized
            val needRefreshChapter = chapters.isEmpty()

            // Show what we have earlier
            mutableState.update {
                val source = sourceManager.getOrStub(manga.source)
                State.Success(
                    manga = manga,
                    source = source,
                    isFromSource = isFromSource,
                    chapters = chapters,
                    // SY -->
                    availableScanlators = if (manga.source == MERGED_SOURCE_ID) {
                        getAvailableScanlators.awaitMerge(mangaId)
                    } else {
                        getAvailableScanlators.await(mangaId)
                    },
                    // SY <--
                    excludedScanlators = getExcludedScanlators.await(mangaId),
                    isRefreshingData = needRefreshInfo || needRefreshChapter,
                    dialog = null,
                    // SY -->
                    showRecommendationsInOverflow = uiPreferences.recommendsInOverflow.get(),
                    showMergeInOverflow = uiPreferences.mergeInOverflow.get(),
                    showMergeWithAnother = smartSearched,
                    mergedData = mergedData,
                    meta = raiseMetadata(meta, source),
                    pagePreviewsState = if (source.getMainSource() is PagePreviewSource) {
                        getPagePreviews(manga, source)
                        PagePreviewState.Loading
                    } else {
                        PagePreviewState.Unused
                    },
                    alwaysShowReadingProgress =
                    readerPreferences.preserveReadingPosition.get() && manga.isEhBasedManga(),
                    previewsRowCount = uiPreferences.previewsRowCount.get(),
                    // SY <--
                )
            }

            // Start observe tracking since it only needs mangaId
            observeTrackers()

            // Fetch info-chapters when needed
            if ((needRefreshInfo || needRefreshChapter) && screenModelScope.isActive) {
                fetchAllFromSource(
                    manualFetch = false,
                    fetchDetails = needRefreshInfo,
                    fetchChapters = needRefreshChapter,
                )
            }

            // Initial loading finished
            updateSuccessState { it.copy(isRefreshingData = false) }
        }
    }

    // Helper function to update the UI state only if it's currently in success state.
    internal fun updateSuccessState(func: (State.Success) -> State.Success) {
        mutableState.update {
            when (it) {
                State.Loading -> it
                is State.Success -> func(it)
            }
        }
    }

    fun fetchAllFromSource(manualFetch: Boolean = true) {
        screenModelScope.launch {
            updateSuccessState { it.copy(isRefreshingData = true) }
            fetchAllFromSource(
                manualFetch = manualFetch,
                fetchDetails = true,
                fetchChapters = true,
            )
            updateSuccessState { it.copy(isRefreshingData = false) }
        }
    }

    private suspend fun fetchAllFromSource(
        manualFetch: Boolean,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ) {
        val state = successState ?: return
        try {
            withUIContext {
                val update = updateMangaFromRemote(
                    source = state.source,
                    manga = state.manga,
                    fetchDetails = fetchDetails,
                    fetchChapters = fetchChapters,
                    manualFetch = manualFetch,
                )
                    .getOrThrow()

                if (manualFetch) {
                    downloads.downloadNewChapters(update.newChapters)
                }
            }
        } catch (_: CancellationException) {
            // ignore
        } catch (_: NoChaptersException) {
            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = context.stringResource(MR.strings.no_chapters_error))
            }
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
            val message = with(context) { expected.formattedMessage }

            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
        }
    }

    // SY -->
    private fun raiseMetadata(flatMetadata: FlatMetadata?, source: Source): RaisedSearchMetadata? {
        val metaClass = source.getMainSource<MetadataSource<*, *>>()?.metaClass ?: return null
        return flatMetadata?.raise(metaClass)
    }

    fun updateMangaInfo(
        title: String?,
        author: String?,
        artist: String?,
        thumbnailUrl: String?,
        description: String?,
        tags: List<String>?,
        status: Long?,
    ) {
        val state = successState ?: return
        var manga = state.manga
        if (state.manga.isLocal()) {
            val newTitle = if (title.isNullOrBlank()) manga.url else title.trim()
            val newAuthor = author?.trimOrNull()
            val newArtist = artist?.trimOrNull()
            val newThumbnailUrl = thumbnailUrl?.trimOrNull()
            val newDesc = description?.trimOrNull()
            manga = manga.copy(
                ogTitle = newTitle,
                ogAuthor = author?.trimOrNull(),
                ogArtist = artist?.trimOrNull(),
                ogThumbnailUrl = thumbnailUrl?.trimOrNull(),
                ogDescription = description?.trimOrNull(),
                ogGenre = tags?.nullIfEmpty(),
                ogStatus = status ?: 0,
                lastUpdate = manga.lastUpdate + 1,
            )
            (sourceManager.get(LocalSource.ID) as LocalSource).updateMangaInfo(manga.toSManga())
            screenModelScope.launchNonCancellable {
                updateManga.await(
                    MangaUpdate(
                        manga.id,
                        title = newTitle,
                        author = newAuthor,
                        artist = newArtist,
                        thumbnailUrl = newThumbnailUrl,
                        description = newDesc,
                        genre = tags,
                        status = status,
                    ),
                )
            }
        } else {
            val genre = if (!tags.isNullOrEmpty() && tags != state.manga.ogGenre) {
                tags
            } else {
                null
            }
            setCustomMangaInfo.set(
                CustomMangaInfo(
                    state.manga.id,
                    title?.trimOrNull(),
                    author?.trimOrNull(),
                    artist?.trimOrNull(),
                    thumbnailUrl?.trimOrNull(),
                    description?.trimOrNull(),
                    genre,
                    status.takeUnless { it == state.manga.ogStatus },
                ),
            )
            manga = manga.copy(lastUpdate = manga.lastUpdate + 1)
        }

        updateSuccessState { successState ->
            successState.copy(manga = manga)
        }
    }

    // SY <--

    // Manga info - start

    fun toggleFavorite() {
        toggleFavorite(
            onRemoved = {
                screenModelScope.launch {
                    if (downloads.hasDownloads()) {
                        val result = snackbarHostState.showSnackbar(
                            message = context.stringResource(MR.strings.delete_downloads_for_manga),
                            actionLabel = context.stringResource(MR.strings.action_delete),
                            withDismissAction = true,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            downloads.deleteDownloads()
                        }
                    }
                }
            },
        )
    }

    /**
     * Update favorite status of manga, (removes / adds) manga (to / from) library.
     */
    fun toggleFavorite(onRemoved: () -> Unit, checkDuplicate: Boolean = true) {
        library.toggleFavorite(onRemoved, checkDuplicate)
    }

    // Manga info - end

    // Chapters list - start

    private fun List<Chapter>.toChapterListItems(
        manga: Manga,
        mergedData: MergedMangaData?,
    ): List<ChapterList.Item> {
        val isLocal = manga.isLocal()
        // SY -->
        val isExhManga = manga.isEhBasedManga()
        val enabledLanguages = Injekt.get<SourcePreferences>().enabledLanguages.get()
            .filterNot { it in listOf("all", "other") }
        // SY <--
        return map { chapter ->
            val activeDownload = if (isLocal) {
                null
            } else {
                downloadManager.getQueuedDownloadOrNull(chapter.id)
            }

            // SY -->
            @Suppress("NAME_SHADOWING")
            val manga = mergedData?.manga?.get(chapter.mangaId) ?: manga
            val source = mergedData?.sources?.find { manga.source == it.id }?.takeIf { mergedData.sources.size > 2 }
            // SY <--
            val downloaded = if (manga.isLocal()) {
                true
            } else {
                downloadManager.isChapterDownloaded(
                    chapter.name,
                    chapter.scanlator,
                    chapter.url,
                    /* SY --> */ manga.ogTitle, /* <-- SY */
                    manga.source,
                )
            }
            val downloadState = when {
                activeDownload != null -> activeDownload.status
                downloaded -> Download.State.DOWNLOADED
                else -> Download.State.NOT_DOWNLOADED
            }

            ChapterList.Item(
                chapter = chapter,
                downloadState = downloadState,
                downloadProgress = activeDownload?.progress ?: 0,
                selected = chapter.id in selection.selectedChapterIds,
                // SY -->
                sourceName = source?.getNameForMangaInfo(enabledLanguages = enabledLanguages),
                showScanlator = !isExhManga,
                // SY <--
            )
        }
    }

    // SY -->
    private fun getPagePreviews(manga: Manga, source: Source) {
        screenModelScope.launchIO {
            when (val result = getPagePreviews.await(manga, source, 1)) {
                is GetPagePreviews.Result.Error -> updateSuccessState {
                    it.copy(pagePreviewsState = PagePreviewState.Error(result.error))
                }
                is GetPagePreviews.Result.Success -> updateSuccessState {
                    it.copy(pagePreviewsState = PagePreviewState.Success(result.pagePreviews))
                }
                GetPagePreviews.Result.Unused -> updateSuccessState {
                    it.copy(pagePreviewsState = PagePreviewState.Unused)
                }
            }
        }
    }
    // SY <--

    // Chapters list - end

    // Track sheet - start

    private fun observeTrackers() {
        tracking.observe()
    }

    // Track sheet - end

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
                processedChapters.insertSeparators { before, after ->
                    val (lowerChapter, higherChapter) = if (manga.sortDescending()) {
                        after to before
                    } else {
                        before to after
                    }
                    if (higherChapter == null) return@insertSeparators null

                    if (lowerChapter == null) {
                        floor(higherChapter.chapter.chapterNumber)
                            .toInt()
                            .minus(1)
                            .coerceAtLeast(0)
                    } else {
                        calculateChapterGap(higherChapter.chapter, lowerChapter.chapter)
                    }
                        .takeIf { it > 0 }
                        ?.let { missingCount ->
                            ChapterList.MissingCount(
                                id = "${lowerChapter?.id}-${higherChapter.id}",
                                count = missingCount,
                            )
                        }
                }
            }

            val scanlatorFilterActive: Boolean
                get() = excludedScanlators.intersect(availableScanlators).isNotEmpty()

            val filterActive: Boolean
                get() = scanlatorFilterActive || manga.chaptersFiltered()
        }
    }
}

// Applies the view filters to the list of chapters obtained from the database.
// @return the chapters filtered and sorted.
private fun List<ChapterList.Item>.applyFilters(manga: Manga): Sequence<ChapterList.Item> {
    val isLocalManga = manga.isLocal()
    val unreadFilter = manga.unreadFilter
    val downloadedFilter = manga.downloadedFilter
    val bookmarkedFilter = manga.bookmarkedFilter
    return asSequence()
        .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalManga } }
        .sortedWith { (chapter1), (chapter2) -> getChapterSort(manga).invoke(chapter1, chapter2) }
}

internal data class MergedMangaData(
    val references: List<MergedMangaReference>,
    val manga: Map<Long, Manga>,
    val sources: List<Source>,
)

@Immutable
internal sealed class ChapterList {
    @Immutable
    data class MissingCount(
        val id: String,
        val count: Int,
    ) : ChapterList()

    @Immutable
    data class Item(
        val chapter: Chapter,
        val downloadState: Download.State,
        val downloadProgress: Int,
        val selected: Boolean = false,
        // SY -->
        val sourceName: String?,
        val showScanlator: Boolean,
        // SY <--
    ) : ChapterList() {
        val id = chapter.id
        val isDownloaded = downloadState == Download.State.DOWNLOADED
    }
}

// SY -->
internal sealed interface PagePreviewState {
    data object Unused : PagePreviewState
    data object Loading : PagePreviewState
    data class Success(val pagePreviews: List<PagePreview>) : PagePreviewState
    data class Error(val error: Throwable) : PagePreviewState
}
// SY <--
