package eu.kanade.tachiyomi.ui.reader

import android.net.Uri
import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.manga.interactor.SetMangaViewerFlags
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.addDownloadsToStartOfQueue
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.unref
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import exh.metadata.metadata.RaisedSearchMetadata
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import tachiyomi.core.common.storage.UniFileTempFileManager
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// Presenter used by the activity to perform background operations.
internal class ReaderViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    internal val sourceManager: SourceManager = Injekt.get(),
    internal val downloadManager: DownloadManager = Injekt.get(),
    internal val downloadProvider: DownloadProvider = Injekt.get(),
    private val tempFileManager: UniFileTempFileManager = Injekt.get(),
    val readerPreferences: ReaderPreferences = Injekt.get(),
    internal val basePreferences: BasePreferences = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    internal val getManga: GetManga = Injekt.get(),
    internal val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    internal val updateChapter: UpdateChapter = Injekt.get(),
    private val setMangaViewerFlags: SetMangaViewerFlags = Injekt.get(),
    private val getIncognitoState: GetIncognitoState = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    // SY -->
    private val syncPreferences: SyncPreferences = Injekt.get(),
    internal val uiPreferences: UiPreferences = Injekt.get(),
    internal val getFlatMetadataById: GetFlatMetadataById = Injekt.get(),
    internal val getMergedMangaById: GetMergedMangaById = Injekt.get(),
    internal val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    internal val getMergedChaptersByMangaId: GetMergedChaptersByMangaId = Injekt.get(),
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
    internal var chapterId = savedState.get<Long>("chapter_id") ?: -1L
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
    internal val chapterList by lazy { buildChapterList() }

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
        observeCurrentChapter()
        // SY -->
        observeAutoscrollFrequency()
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

    /** Applies [func] to the state; the extension files reach the protected flow through it. */
    internal fun updateState(func: (State) -> State) {
        mutableState.update(func)
    }

    /**
     * Whether this presenter is initialized yet.
     */
    fun needsInit(): Boolean = manga == null

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
