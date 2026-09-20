package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.net.Uri
import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.chapter.model.toDbChapter
import eu.kanade.domain.manga.interactor.SetMangaViewerFlags
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.addDownloadsToStartOfQueue
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.unref
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.util.chapter.filterDownloaded
import eu.kanade.tachiyomi.util.chapter.removeDuplicates
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.raise
import exh.source.MERGED_SOURCE_ID
import exh.source.getMainSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import tachiyomi.core.common.storage.UniFileTempFileManager
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.bookmarkedFilterRaw
import tachiyomi.domain.manga.model.downloadedFilterRaw
import tachiyomi.domain.manga.model.unreadFilterRaw
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// Presenter used by the activity to perform background operations.
private const val MAX_PAGE_INPUT = 9999

// Download-ahead starts once a quarter of the chapter has been read.
private const val DOWNLOAD_AHEAD_THRESHOLD = 0.25

internal class ReaderViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    internal val sourceManager: SourceManager = Injekt.get(),
    internal val downloadManager: DownloadManager = Injekt.get(),
    private val downloadProvider: DownloadProvider = Injekt.get(),
    private val tempFileManager: UniFileTempFileManager = Injekt.get(),
    val readerPreferences: ReaderPreferences = Injekt.get(),
    private val basePreferences: BasePreferences = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    internal val updateChapter: UpdateChapter = Injekt.get(),
    private val setMangaViewerFlags: SetMangaViewerFlags = Injekt.get(),
    private val getIncognitoState: GetIncognitoState = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    // SY -->
    private val syncPreferences: SyncPreferences = Injekt.get(),
    private val uiPreferences: UiPreferences = Injekt.get(),
    private val getFlatMetadataById: GetFlatMetadataById = Injekt.get(),
    private val getMergedMangaById: GetMergedMangaById = Injekt.get(),
    private val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    private val getMergedChaptersByMangaId: GetMergedChaptersByMangaId = Injekt.get(),
    // SY <--
) : ViewModel() {

    internal val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    internal val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    /**
     * The manga loaded in the reader. It can be null when instantiated for a short time.
     */
    val manga: Manga?
        get() = state.value.manga

    // The chapter id of the currently loaded chapter. Used to restore from process kill.
    private var chapterId = savedState.get<Long>("chapter_id") ?: -1L
        set(value) {
            savedState["chapter_id"] = value
            field = value
        }

    // The visible page index of the currently loaded chapter. Used to restore from process kill.
    internal var chapterPageIndex = savedState.get<Int>("page_index") ?: -1
        set(value) {
            savedState["page_index"] = value
            field = value
        }

    // The chapter loader for the loaded manga. It'll be null until [manga] is set.
    internal var loader: ChapterLoader? = null

    internal var chapterToDownload: Download? = null

    internal val unfilteredChapterList by lazy {
        val manga = manga!!
        runBlocking { getChaptersByMangaId.await(manga.id, applyScanlatorFilter = false) }
    }

    // Chapter list for the active manga. It's retrieved lazily and should be accessed for the first
    // time in a background thread to avoid blocking the UI.
    internal val chapterList by lazy {
        val manga = manga!!
        // SY -->
        val (chapters, mangaMap) = runBlocking {
            if (manga.source == MERGED_SOURCE_ID) {
                getMergedChaptersByMangaId.await(manga.id, applyScanlatorFilter = true) to
                    getMergedMangaById.await(manga.id)
                        .associateBy { it.id }
            } else {
                getChaptersByMangaId.await(manga.id, applyScanlatorFilter = true) to null
            }
        }
        fun isChapterDownloaded(chapter: Chapter): Boolean {
            val chapterManga = mangaMap?.get(chapter.mangaId) ?: manga
            return downloadManager.isChapterDownloaded(
                chapterName = chapter.name,
                chapterScanlator = chapter.scanlator,
                chapterUrl = chapter.url,
                mangaTitle = chapterManga.ogTitle,
                sourceId = chapterManga.source,
            )
        }
        // SY <--

        val selectedChapter = chapters.find { it.id == chapterId }
            ?: error("Requested chapter of id $chapterId not found in chapter list")

        val chaptersForReader = if (readerPreferences.skipRead.get() || readerPreferences.skipFiltered.get()) {
            val filteredChapters = chapters.filterNot {
                when {
                    readerPreferences.skipRead.get() && it.read -> {
                        true
                    }
                    readerPreferences.skipFiltered.get() -> {
                        (manga.unreadFilterRaw == Manga.CHAPTER_SHOW_READ && !it.read) ||
                            (manga.unreadFilterRaw == Manga.CHAPTER_SHOW_UNREAD && it.read) ||
                            // SY -->
                            (
                                manga.downloadedFilterRaw == Manga.CHAPTER_SHOW_DOWNLOADED &&
                                    !isChapterDownloaded(it)
                                ) ||
                            (
                                manga.downloadedFilterRaw == Manga.CHAPTER_SHOW_NOT_DOWNLOADED &&
                                    isChapterDownloaded(it)
                                ) ||
                            // SY <--
                            (manga.bookmarkedFilterRaw == Manga.CHAPTER_SHOW_BOOKMARKED && !it.bookmark) ||
                            (manga.bookmarkedFilterRaw == Manga.CHAPTER_SHOW_NOT_BOOKMARKED && it.bookmark)
                    }
                    else -> {
                        false
                    }
                }
            }

            if (filteredChapters.any { it.id == chapterId }) {
                filteredChapters
            } else {
                filteredChapters + listOf(selectedChapter)
            }
        } else {
            chapters
        }

        chaptersForReader
            .sortedWith(getChapterSort(manga, sortDescending = false))
            .run {
                if (readerPreferences.skipDupe.get()) {
                    removeDuplicates(selectedChapter)
                } else {
                    this
                }
            }
            .run {
                if (basePreferences.downloadedOnly.get()) {
                    filterDownloaded(manga, mangaMap)
                } else {
                    this
                }
            }
            .map { it.toDbChapter() }
            .map(::ReaderChapter)
    }

    internal val incognitoMode: Boolean by lazy { getIncognitoState.await(manga?.source) }
    val images = ReaderImageActions(this, readerPreferences)
    internal val viewerSettings =
        ReaderViewerSettings(this, readerPreferences, sourceManager, getManga, setMangaViewerFlags)
    val progress = ReaderProgress(this, trackPreferences, libraryPreferences, syncPreferences)
    internal val chapterDownloads = ReaderChapterDownloads(
        this,
        readerPreferences,
        downloadPreferences,
        downloadManager,
        tempFileManager,
        getNextChapters,
    )

    init {
        // To save state
        state.map { it.viewerChapters?.currChapter }
            .distinctUntilChanged()
            .filterNotNull()
            // SY -->
            .drop(1) // allow the loader to set the first page and chapter id
            // SY <-
            .onEach { currentChapter ->
                if (chapterPageIndex >= 0) {
                    // Restore from SavedState
                    currentChapter.requestedPage = chapterPageIndex
                } else if (!currentChapter.chapter.read) {
                    currentChapter.requestedPage = currentChapter.chapter.lastPageRead
                }
                chapterId = currentChapter.chapter.id!!
            }
            .launchIn(viewModelScope)

        // SY -->
        state.mapLatest { it.ehAutoscrollFreq }
            .distinctUntilChanged()
            .drop(1)
            .onEach { text ->
                val parsed = text.toDoubleOrNull()

                if (parsed == null || parsed <= 0 || parsed > MAX_PAGE_INPUT) {
                    readerPreferences.autoscrollInterval.set(-1f)
                    mutableState.update { it.copy(isAutoScrollEnabled = false) }
                } else {
                    readerPreferences.autoscrollInterval.set(parsed.toFloat())
                    mutableState.update { it.copy(isAutoScrollEnabled = true) }
                }
            }
            .launchIn(viewModelScope)
        // SY <--
    }

    override fun onCleared() {
        val currentChapters = state.value.viewerChapters
        if (currentChapters != null) {
            currentChapters.unref()
            chapterToDownload?.let {
                downloadManager.addDownloadsToStartOfQueue(listOf(it))
            }
        }
    }

    /**
     * Called when the user pressed the back button and is going to leave the reader. Used to
     * trigger deletion of the downloaded chapters.
     */
    fun onActivityFinish() {
        chapterDownloads.deletePendingChapters()
    }

    /**
     * Whether this presenter is initialized yet.
     */
    fun needsInit(): Boolean = manga == null

    /**
     * Initializes this presenter with the given [mangaId] and [initialChapterId]. This method will
     * fetch the manga from the database and initialize the initial chapter.
     */
    suspend fun init(mangaId: Long, initialChapterId: Long /* SY --> */, page: Int?/* SY <-- */): Result<Boolean> {
        if (!needsInit()) return Result.success(true)
        return withIOContext {
            try {
                val manga = getManga.await(mangaId)
                if (manga != null) {
                    // SY -->
                    sourceManager.isInitialized.first { it }
                    val source = sourceManager.getOrStub(manga.source)
                    val merged = mergedDataFor(manga, source)
                    publishInitialState(manga, source, merged)
                    // SY <--
                    if (chapterId == -1L) chapterId = initialChapterId

                    val newLoader = ChapterLoader(
                        services = ChapterLoader.Services(
                            context = Injekt.get<Application>(),
                            downloadManager = downloadManager,
                            downloadProvider = downloadProvider,
                            sourceManager = sourceManager,
                            readerPrefs = readerPreferences,
                        ),
                        manga = manga,
                        source = source,
                        /* SY --> */ merged = merged, /* SY <-- */
                    )
                    loader = newLoader
                    loadChapter(
                        newLoader,
                        chapterList.first { chapterId == it.chapter.id },
                        /* SY --> */ page /* SY <-- */,
                    )
                    Result.success(true)
                } else {
                    // Unlikely but okay
                    Result.success(false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (expected: Throwable) {
                // Rethrown (or wrapped) whatever the cause.
                Result.failure(expected)
            }
        }
    }

    // SY -->

    // The merged-source references and entries backing [manga], empty for any other source.
    private suspend fun mergedDataFor(manga: Manga, source: Source): ChapterLoader.MergedData {
        if (source !is MergedSource) return ChapterLoader.MergedData(emptyList(), emptyMap())
        val references = getMergedReferencesById.await(manga.id)
        val mergedManga = getMergedMangaById.await(manga.id).associateBy { it.id }
        return ChapterLoader.MergedData(references, mergedManga)
    }

    private suspend fun publishInitialState(manga: Manga, source: Source, merged: ChapterLoader.MergedData) {
        val metadataSource = source.getMainSource<MetadataSource<*, *>>()
        val metadata = metadataSource?.let { getFlatMetadataById.await(manga.id)?.raise(it.metaClass) }
        val relativeTime = uiPreferences.relativeTime.get()
        val autoScrollFreq = readerPreferences.autoscrollInterval.get()
        mutableState.update {
            it.copy(
                manga = manga,
                meta = metadata,
                mergedManga = merged.manga,
                dateRelativeTime = relativeTime,
                ehAutoscrollFreq = if (autoScrollFreq == -1f) "" else autoScrollFreq.toString(),
                isAutoScrollEnabled = autoScrollFreq != -1f,
            )
        }
    }
    // SY <--

    // SY -->
    fun getChapters(): List<ReaderChapterItem> {
        val currentChapter = getCurrentChapter()

        return chapterList.map {
            ReaderChapterItem(
                chapter = it.chapter.toDomainChapter()!!,
                manga = manga!!,
                isCurrent = it.chapter.id == currentChapter?.chapter?.id,
                dateFormat = UiPreferences.dateFormat(uiPreferences.dateFormat.get()),
            )
        }
    }
    // SY <--

    fun onViewerLoaded(viewer: Viewer?) {
        mutableState.update {
            it.copy(viewer = viewer)
        }
    }

    /**
     * Called every time a page changes on the reader. Used to mark the flag of chapters being
     * read, update tracking services, enqueue downloaded chapter deletion, and updating the active chapter if this
     * [page]'s chapter is different from the currently active.
     */
    fun onPageSelected(page: ReaderPage, currentPageText: String /* SY --> */, hasExtraPage: Boolean /* SY <-- */) {
        // InsertPage doesn't change page progress
        if (page is InsertPage) {
            return
        }

        // SY -->
        mutableState.update { it.copy(currentPageText = currentPageText) }
        // SY <--

        val selectedChapter = page.chapter
        val pages = selectedChapter.pages ?: return

        // Save last page read and mark as read if needed
        viewModelScope.launchNonCancellable {
            progress.updateChapterProgress(selectedChapter, page/* SY --> */, hasExtraPage/* SY <-- */)
        }

        if (selectedChapter != getCurrentChapter()) {
            logcat { "Setting ${selectedChapter.chapter.url} as active" }
            loadNewChapter(selectedChapter)
        }

        val inDownloadRange = page.number.toDouble() / pages.size > DOWNLOAD_AHEAD_THRESHOLD
        if (inDownloadRange) {
            chapterDownloads.downloadNextChapters()
        }

        eventChannel.trySend(Event.PageChanged)
    }

    // Returns the currently active chapter.
    internal fun getCurrentChapter(): ReaderChapter? = state.value.currentChapter

    // SY <--

    // SY <--

    enum class SetAsCoverResult {
        Success,
        AddToLibraryFirst,
        Error,
    }

    sealed interface SaveImageResult {
        class Success(val uri: Uri) : SaveImageResult
        class Error(val error: Throwable) : SaveImageResult
    }

    @Immutable
    data class State(
        val manga: Manga? = null,
        val viewerChapters: ViewerChapters? = null,
        val bookmarked: Boolean = false,
        val isLoadingAdjacentChapter: Boolean = false,
        val currentPage: Int = -1,

        /**
         * Viewer used to display the pages (pager, webtoon, ...).
         */
        val viewer: Viewer? = null,
        val dialog: Dialog? = null,
        val menuVisible: Boolean = false,
        @IntRange(from = -100, to = 100) val brightnessOverlayValue: Int = 0,

        // SY -->
        val currentPageText: String = "",
        val meta: RaisedSearchMetadata? = null,
        val mergedManga: Map<Long, Manga>? = null,
        val ehUtilsVisible: Boolean = false,
        val lastShiftDoubleState: Boolean? = null,
        val indexPageToShift: Int? = null,
        val indexChapterToShift: Long? = null,
        val doublePages: Boolean = false,
        val dateRelativeTime: Boolean = true,
        val autoScroll: Boolean = false,
        val isAutoScrollEnabled: Boolean = false,
        val ehAutoscrollFreq: String = "",
        // SY <--
    ) {
        val currentChapter: ReaderChapter?
            get() = viewerChapters?.currChapter

        val totalPages: Int
            get() = currentChapter?.pages?.size ?: -1
    }

    sealed interface Dialog {
        data object Loading : Dialog
        data object Settings : Dialog
        data object ReadingModeSelect : Dialog
        data object OrientationModeSelect : Dialog

        // SY -->
        data object ChapterList : Dialog
        // SY <--

        data class PageActions(
            val page: ReaderPage/* SY --> */,
            val extraPage: ReaderPage? = null, /* SY <-- */
        ) : Dialog

        // SY -->
        data object AutoScrollHelp : Dialog
        data object RetryAllHelp : Dialog
        data object BoostPageHelp : Dialog
        // SY <--
    }

    sealed interface Event {
        data object ReloadViewerChapters : Event
        data object PageChanged : Event
        data class SetOrientation(val orientation: Int) : Event
        data class SetCoverResult(val result: SetAsCoverResult) : Event

        data class SavedImage(val result: SaveImageResult) : Event
        data class ShareImage(
            val uri: Uri,
            val page: ReaderPage/* SY --> */,
            val secondPage: ReaderPage? = null, /* SY <-- */
        ) : Event
        data class CopyImage(val uri: Uri) : Event
    }
}
