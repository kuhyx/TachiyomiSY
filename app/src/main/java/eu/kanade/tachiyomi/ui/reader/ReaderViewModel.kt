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
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
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
import logcat.LogPriority
import tachiyomi.core.common.storage.UniFileTempFileManager
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
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
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// Presenter used by the activity to perform background operations.
private const val MAX_PAGE_INPUT = 9999

// Download-ahead starts once a quarter of the chapter has been read.
private const val DOWNLOAD_AHEAD_THRESHOLD = 0.25

internal class ReaderViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadProvider: DownloadProvider = Injekt.get(),
    private val tempFileManager: UniFileTempFileManager = Injekt.get(),
    val readerPreferences: ReaderPreferences = Injekt.get(),
    private val basePreferences: BasePreferences = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val updateChapter: UpdateChapter = Injekt.get(),
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
    private var loader: ChapterLoader? = null

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
    private val images = ReaderImageActions(this, readerPreferences)
    private val viewerSettings =
        ReaderViewerSettings(this, readerPreferences, sourceManager, getManga, setMangaViewerFlags)
    private val progress = ReaderProgress(this, trackPreferences, libraryPreferences, syncPreferences)
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
                    val metadataSource = source.getMainSource<MetadataSource<*, *>>()
                    val metadata = metadataSource?.let { getFlatMetadataById.await(mangaId)?.raise(it.metaClass) }
                    val mergedReferences = if (source is MergedSource) {
                        runBlocking {
                            getMergedReferencesById.await(manga.id)
                        }
                    } else {
                        emptyList()
                    }
                    val mergedManga = if (source is MergedSource) {
                        runBlocking {
                            getMergedMangaById.await(manga.id)
                        }.associateBy { it.id }
                    } else {
                        emptyMap()
                    }
                    val relativeTime = uiPreferences.relativeTime.get()
                    val autoScrollFreq = readerPreferences.autoscrollInterval.get()
                    // SY <--
                    mutableState.update {
                        it.copy(
                            manga = manga,
                            /* SY --> */
                            meta = metadata,
                            mergedManga = mergedManga,
                            dateRelativeTime = relativeTime,
                            ehAutoscrollFreq = if (autoScrollFreq == -1f) {
                                ""
                            } else {
                                autoScrollFreq.toString()
                            },
                            isAutoScrollEnabled = autoScrollFreq != -1f,
                            /* SY <-- */
                        )
                    }
                    if (chapterId == -1L) chapterId = initialChapterId

                    val context = Injekt.get<Application>()
                    // val source = sourceManager.getOrStub(manga.source)
                    loader = ChapterLoader(
                        context = context,
                        downloadManager = downloadManager,
                        downloadProvider = downloadProvider,
                        manga = manga,
                        source = source, /* SY --> */
                        sourceManager = sourceManager,
                        readerPrefs = readerPreferences,
                        mergedReferences = mergedReferences,
                        mergedManga = mergedManga, /* SY <-- */
                    )

                    loadChapter(
                        loader!!,
                        chapterList.first { chapterId == it.chapter.id },
                        /* SY --> */page, /* SY <-- */
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

    // Loads the given [chapter] with this [loader] and updates the currently active chapters.
    // Callers must handle errors.
    private suspend fun loadChapter(
        loader: ChapterLoader,
        chapter: ReaderChapter,
        // SY -->
        page: Int? = null,
        // SY <--
    ): ViewerChapters {
        loader.loadChapter(chapter /* SY --> */, page/* SY <-- */)

        val chapterPos = chapterList.indexOf(chapter)
        val newChapters = ViewerChapters(
            chapter,
            chapterList.getOrNull(chapterPos - 1),
            chapterList.getOrNull(chapterPos + 1),
        )

        withUIContext {
            mutableState.update {
                // Add new references first to avoid unnecessary recycling
                newChapters.ref()
                it.viewerChapters?.unref()

                chapterToDownload = chapterDownloads.cancelQueuedDownloads(newChapters.currChapter)
                it.copy(
                    viewerChapters = newChapters,
                    bookmarked = newChapters.currChapter.chapter.bookmark,
                )
            }
        }
        return newChapters
    }

    // Called when the user changed to the given [chapter] when changing pages from the viewer.
    // It's used only to set this chapter as active.
    private fun loadNewChapter(chapter: ReaderChapter) {
        val loader = loader ?: return

        viewModelScope.launchIO {
            logcat { "Loading ${chapter.chapter.url}" }

            updateHistory()
            restartReadTimer()

            try {
                loadChapter(loader, chapter)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (expected: Throwable) {
                // Logged whatever the cause; the caller carries on.
                logcat(LogPriority.ERROR, expected)
            }
        }
    }

    fun loadNewChapterFromDialog(chapter: Chapter) {
        viewModelScope.launchIO {
            val newChapter = chapterList.firstOrNull { it.chapter.id == chapter.id }
            if (newChapter != null) {
                loadAdjacent(newChapter)
            }
        }
    }

    // Called when the user is going to load the prev/next chapter through the toolbar buttons.
    private suspend fun loadAdjacent(chapter: ReaderChapter) {
        val loader = loader ?: return

        logcat { "Loading adjacent ${chapter.chapter.url}" }

        mutableState.update { it.copy(isLoadingAdjacentChapter = true) }
        try {
            withIOContext {
                loadChapter(loader, chapter)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (expected: Throwable) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
        } finally {
            mutableState.update { it.copy(isLoadingAdjacentChapter = false) }
        }
    }

    /**
     * Called when the viewers decide it's a good time to preload a [chapter] and improve the UX so
     * that the user doesn't have to wait too long to continue reading.
     */
    suspend fun preload(chapter: ReaderChapter) {
        if (chapter.state is ReaderChapter.State.Loaded || chapter.state == ReaderChapter.State.Loading) {
            return
        }

        if (chapter.pageLoader?.isLocal == false) {
            val manga = manga ?: return
            val dbChapter = chapter.chapter
            val isDownloaded = downloadManager.isChapterDownloaded(
                dbChapter.name,
                dbChapter.scanlator,
                dbChapter.url,
                /* SY --> */ manga.ogTitle /* SY <-- */,
                manga.source,
                skipCache = true,
            )
            if (isDownloaded) {
                chapter.state = ReaderChapter.State.Wait
            }
        }

        if (chapter.state != ReaderChapter.State.Wait && chapter.state !is ReaderChapter.State.Error) {
            return
        }

        val loader = loader ?: return
        try {
            logcat { "Preloading ${chapter.chapter.url}" }
            loader.loadChapter(chapter)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (expected: Throwable) {
            // Rethrown (or wrapped) whatever the cause.
            return
        }
        eventChannel.trySend(Event.ReloadViewerChapters)
    }

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

    fun restartReadTimer() {
        progress.restartReadTimer()
    }

    /**
     * Saves the chapter last read history if incognito mode isn't on.
     */
    suspend fun updateHistory() {
        progress.updateHistory()
    }

    /**
     * Called from the activity to load and set the next chapter as active.
     */
    suspend fun loadNextChapter() {
        val nextChapter = state.value.viewerChapters?.nextChapter ?: return
        loadAdjacent(nextChapter)
    }

    /**
     * Called from the activity to load and set the previous chapter as active.
     */
    suspend fun loadPreviousChapter() {
        val prevChapter = state.value.viewerChapters?.prevChapter ?: return
        loadAdjacent(prevChapter)
    }

    // Returns the currently active chapter.
    internal fun getCurrentChapter(): ReaderChapter? = state.value.currentChapter

    fun getSource() = manga?.source?.let { sourceManager.getOrStub(it) } as? HttpSource

    fun getChapterUrl(): String? {
        val sChapter = getCurrentChapter()?.chapter ?: return null
        val source = getSource() ?: return null

        return try {
            source.getChapterUrl(sChapter)
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
            null
        }
    }

    /**
     * Bookmarks the currently active chapter.
     */
    fun toggleChapterBookmark() {
        val chapter = getCurrentChapter()?.chapter ?: return
        val bookmarked = !chapter.bookmark
        chapter.bookmark = bookmarked

        viewModelScope.launchNonCancellable {
            updateChapter.await(
                ChapterUpdate(
                    id = chapter.id!!,
                    bookmark = bookmarked,
                ),
            )
        }

        mutableState.update {
            it.copy(
                bookmarked = bookmarked,
            )
        }
    }

    // SY -->
    fun toggleBookmark(chapterId: Long, bookmarked: Boolean) {
        val chapter = chapterList.find { it.chapter.id == chapterId }?.chapter ?: return
        chapter.bookmark = bookmarked
        viewModelScope.launchNonCancellable {
            updateChapter.await(
                ChapterUpdate(
                    id = chapterId,
                    bookmark = bookmarked,
                ),
            )
        }
    }
    // SY <--

    fun getMangaReadingMode(resolveDefault: Boolean = true): Int = viewerSettings.getMangaReadingMode(resolveDefault)

    fun setMangaReadingMode(readingMode: ReadingMode) {
        viewerSettings.setMangaReadingMode(readingMode)
    }

    fun getMangaOrientation(resolveDefault: Boolean = true): Int = viewerSettings.getMangaOrientation(resolveDefault)

    fun setMangaOrientationType(orientation: ReaderOrientation) {
        viewerSettings.setMangaOrientationType(orientation)
    }

    // SY -->
    fun toggleCropBorders(): Boolean = viewerSettings.toggleCropBorders()
    // SY <--

    fun showMenus(visible: Boolean) {
        mutableState.update { it.copy(menuVisible = visible) }
    }

    // SY -->
    fun showEhUtils(visible: Boolean) {
        mutableState.update { it.copy(ehUtilsVisible = visible) }
    }

    fun setIndexChapterToShift(index: Long?) {
        mutableState.update { it.copy(indexChapterToShift = index) }
    }

    fun setIndexPageToShift(index: Int?) {
        mutableState.update { it.copy(indexPageToShift = index) }
    }

    fun openChapterListDialog() {
        mutableState.update { it.copy(dialog = Dialog.ChapterList) }
    }

    fun setDoublePages(doublePages: Boolean) {
        mutableState.update { it.copy(doublePages = doublePages) }
    }

    fun openAutoScrollHelpDialog() {
        mutableState.update { it.copy(dialog = Dialog.AutoScrollHelp) }
    }

    fun openBoostPageHelp() {
        mutableState.update { it.copy(dialog = Dialog.BoostPageHelp) }
    }

    fun openRetryAllHelp() {
        mutableState.update { it.copy(dialog = Dialog.RetryAllHelp) }
    }

    fun toggleAutoScroll(enabled: Boolean) {
        mutableState.update { it.copy(autoScroll = enabled) }
    }

    fun setAutoScrollFrequency(frequency: String) {
        mutableState.update { it.copy(ehAutoscrollFreq = frequency) }
    }
    // SY <--

    fun showLoadingDialog() {
        mutableState.update { it.copy(dialog = Dialog.Loading) }
    }

    fun openReadingModeSelectDialog() {
        mutableState.update { it.copy(dialog = Dialog.ReadingModeSelect) }
    }

    fun openOrientationSelectDialog() {
        mutableState.update { it.copy(dialog = Dialog.OrientationModeSelect) }
    }

    fun openPageDialog(page: ReaderPage/* SY --> */, extraPage: ReaderPage? = null/* SY <-- */) {
        mutableState.update { it.copy(dialog = Dialog.PageActions(page, extraPage)) }
    }

    fun openSettingsDialog() {
        mutableState.update { it.copy(dialog = Dialog.Settings) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    fun setBrightnessOverlayValue(value: Int) {
        mutableState.update { it.copy(brightnessOverlayValue = value) }
    }

    /**
     * Saves the image of the selected page on the pictures directory and notifies the UI of the result.
     * There's also a notification to allow sharing the image somewhere else or deleting it.
     */
    fun saveImage(useExtraPage: Boolean) {
        images.saveImage(useExtraPage)
    }

    // SY -->
    fun saveImages() {
        images.saveImages()
    }
    // SY <--

    fun shareImage(copyToClipboard: Boolean, useExtraPage: Boolean) {
        images.shareImage(copyToClipboard, useExtraPage)
    }

    // SY -->
    fun shareImages(copyToClipboard: Boolean) {
        images.shareImages(copyToClipboard)
    }
    // SY <--

    fun setAsCover(useExtraPage: Boolean) {
        images.setAsCover(useExtraPage)
    }

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
