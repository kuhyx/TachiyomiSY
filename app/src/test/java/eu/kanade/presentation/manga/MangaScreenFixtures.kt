package eu.kanade.presentation.manga

import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import eu.kanade.tachiyomi.ui.manga.MergedMangaData
import eu.kanade.tachiyomi.ui.manga.PagePreviewState
import exh.metadata.metadata.RaisedSearchMetadata
import io.mockk.every
import io.mockk.mockk
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

/** A plain source [id] named "Plain" in English. */
internal fun plainSource(sourceId: Long = 7L): Source = mockk(relaxed = true) {
    every { id } returns sourceId
    every { name } returns "Plain"
    every { lang } returns "en"
}

/** A chapter of manga 1 numbered [number]. */
internal fun mangaChapter(
    id: Long,
    number: Double = id.toDouble(),
    read: Boolean = false,
    bookmark: Boolean = false,
    lastPageRead: Long = 0L,
    dateUpload: Long = 0L,
    scanlator: String? = null,
): Chapter = Chapter.create().copy(
    id = id,
    mangaId = 1L,
    chapterNumber = number,
    sourceOrder = 100 - id,
    read = read,
    bookmark = bookmark,
    lastPageRead = lastPageRead,
    dateUpload = dateUpload,
    scanlator = scanlator,
    name = "Chapter $id",
    url = "/c/$id",
)

/** The row for [chapter]. */
internal fun chapterItem(
    chapter: Chapter,
    state: Download.State = Download.State.NOT_DOWNLOADED,
    selected: Boolean = false,
    sourceName: String? = null,
    showScanlator: Boolean = false,
): ChapterList.Item = ChapterList.Item(
    chapter = chapter,
    downloadState = state,
    downloadProgress = 0,
    selected = selected,
    sourceName = sourceName,
    showScanlator = showScanlator,
)

/** Manga 1 of [source]. */
internal fun screenManga(
    source: Long = 7L,
    favorite: Boolean = false,
    description: String? = "A *story*",
    genre: List<String>? = listOf("Action", "Drama"),
    notes: String = "",
    fetchInterval: Int = 0,
    flags: Long = 0L,
): Manga = Manga.create().copy(
    id = 1L,
    source = source,
    ogTitle = "Needle",
    ogAuthor = "Ann",
    ogArtist = "Bob",
    ogDescription = description,
    ogGenre = genre,
    notes = notes,
    url = "/m/1",
    favorite = favorite,
    fetchInterval = fetchInterval,
    chapterFlags = flags,
    initialized = true,
)

/** A loaded manga screen with [chapters]; every SY switch off unless given. */
internal fun screenState(
    chapters: List<ChapterList.Item> = emptyList(),
    manga: Manga = screenManga(),
    source: Source = plainSource(),
    meta: RaisedSearchMetadata? = null,
    mergedData: MergedMangaData? = null,
    overflow: Boolean = false,
    mergeWithAnother: Boolean = false,
    previews: PagePreviewState = PagePreviewState.Unused,
    refreshing: Boolean = false,
    alwaysProgress: Boolean = false,
): MangaScreenModel.State.Success = MangaScreenModel.State.Success(
    manga = manga,
    source = source,
    isFromSource = false,
    chapters = chapters,
    availableScanlators = emptySet(),
    excludedScanlators = emptySet(),
    isRefreshingData = refreshing,
    meta = meta,
    mergedData = mergedData,
    showRecommendationsInOverflow = overflow,
    showMergeInOverflow = overflow,
    showMergeWithAnother = mergeWithAnother,
    pagePreviewsState = previews,
    alwaysShowReadingProgress = alwaysProgress,
    previewsRowCount = 1,
)

/** Builds [MangaScreenActions] whose every callback appends a line to [events]. */
internal class RecordingActions {
    val events: MutableList<String> = mutableListOf()

    private fun log(event: String): () -> Unit = { events += event }

    fun build(optional: Boolean = true, rows: Int = 1): MangaScreenActions = MangaScreenActions(
        toolbar = MangaToolbarActions(
            navigateUp = log("up"),
            onFilterButtonClicked = log("filter"),
            onShareClicked = log("share").takeIf { optional },
            onDownloadActionClicked = { action: DownloadAction -> events += "download $action" }.takeIf { optional },
            onRefresh = log("refresh"),
            onMigrateClicked = log("migrate").takeIf { optional },
        ),
        header = MangaHeaderActions(
            onAddToLibraryClicked = log("favorite"),
            onWebViewClicked = log("webview").takeIf { optional },
            onWebViewLongClicked = log("webview long").takeIf { optional },
            onTrackingClicked = log("tracking"),
            onEditFetchIntervalClicked = log("interval").takeIf { optional },
            onEditCategoryClicked = log("category").takeIf { optional },
        ),
        info = MangaInfoActions(
            onCoverClicked = log("cover"),
            onSearch = { query, global -> events += "search $query $global" },
            onTagSearch = { events += "tag $it" },
            onEditNotesClicked = log("notes"),
            onContinueReading = log("continue"),
        ),
        chapters = ChapterRowActions(
            onChapterClicked = { events += "open ${it.id}" },
            onDownloadChapter = { items: List<ChapterList.Item>, action: ChapterDownloadAction ->
                events += "chapter download ${items.map { it.id }} $action"
            }.takeIf { optional },
            onChapterSelected = { item, selected, range -> events += "select ${item.id} $selected $range" },
            onChapterSwipe = { item, action -> events += "swipe ${item.id} $action" },
        ),
        selection = selectionActions(),
        sy = SyMangaActions(
            onMetadataViewerClicked = log("metadata"),
            onEditInfoClicked = log("edit info"),
            onRecommendClicked = log("recommend"),
            merge = MergeActions(
                onMergedSettingsClicked = log("merged settings"),
                onMergeClicked = log("merge"),
                onMergeWithAnotherClicked = log("merge another"),
            ),
            previews = PagePreviewActions(
                onOpenPagePreview = { events += "preview $it" },
                onMorePreviewsClicked = log("more previews"),
                previewsRowCount = rows,
            ),
        ),
    )

    private fun selectionActions() = ChapterSelectionActions(
        onMultiBookmarkClicked = { chapters, bookmarked -> events += "bookmark ${chapters.map { it.id }} $bookmarked" },
        onMultiMarkAsReadClicked = { chapters, read -> events += "read ${chapters.map { it.id }} $read" },
        onMarkPreviousAsReadClicked = { events += "previous ${it.id}" },
        onMultiDeleteClicked = { chapters -> events += "delete ${chapters.map { it.id }}" },
        onAllChapterSelected = { events += "all $it" },
        onInvertSelection = log("invert"),
    )
}
