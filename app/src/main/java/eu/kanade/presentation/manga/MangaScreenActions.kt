package eu.kanade.presentation.manga

import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.ui.manga.ChapterList
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences

// Everything the manga screen can be asked to do, grouped by the part of the screen that raises it,
// so the phone and tablet layouts share one signature.

/** The toolbar and its overflow menu. */
internal data class MangaToolbarActions(
    val navigateUp: () -> Unit,
    val onFilterButtonClicked: () -> Unit,
    val onShareClicked: (() -> Unit)?,
    val onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    val onRefresh: () -> Unit,
    val onMigrateClicked: (() -> Unit)?,
)

/** The favourite / tracking / web-view row under the cover. */
internal data class MangaHeaderActions(
    val onAddToLibraryClicked: () -> Unit,
    val onWebViewClicked: (() -> Unit)?,
    val onWebViewLongClicked: (() -> Unit)?,
    val onTrackingClicked: () -> Unit,
    val onEditFetchIntervalClicked: (() -> Unit)?,
    val onEditCategoryClicked: (() -> Unit)?,
)

/** Cover, description, tags and notes. */
internal data class MangaInfoActions(
    val onCoverClicked: () -> Unit,
    val onSearch: (query: String, global: Boolean) -> Unit,
    val onTagSearch: (String) -> Unit,
    val onEditNotesClicked: () -> Unit,
    val onContinueReading: () -> Unit,
)

/** What a chapter row does when tapped, downloaded, selected or swiped. */
internal data class ChapterRowActions(
    val onChapterClicked: (Chapter) -> Unit,
    val onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    val onChapterSelected: (ChapterList.Item, Boolean, Boolean) -> Unit,
    val onChapterSwipe: (ChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
)

/** The bottom action menu over a chapter selection. */
internal data class ChapterSelectionActions(
    val onMultiBookmarkClicked: (List<Chapter>, bookmarked: Boolean) -> Unit,
    val onMultiMarkAsReadClicked: (List<Chapter>, markAsRead: Boolean) -> Unit,
    val onMarkPreviousAsReadClicked: (Chapter) -> Unit,
    val onMultiDeleteClicked: (List<Chapter>) -> Unit,
    val onAllChapterSelected: (Boolean) -> Unit,
    val onInvertSelection: () -> Unit,
)

// SY -->
internal data class MergeActions(
    val onMergedSettingsClicked: () -> Unit,
    val onMergeClicked: () -> Unit,
    val onMergeWithAnotherClicked: () -> Unit,
)

/** The fork's additions: metadata viewer, merging, recommendations and page previews. */
internal data class SyMangaActions(
    val onMetadataViewerClicked: () -> Unit,
    val onEditInfoClicked: () -> Unit,
    val onRecommendClicked: () -> Unit,
    val merge: MergeActions,
    val previews: PagePreviewActions,
)

internal data class PagePreviewActions(
    val onOpenPagePreview: (Int) -> Unit,
    val onMorePreviewsClicked: () -> Unit,
    val previewsRowCount: Int,
)
// SY <--

internal data class MangaScreenActions(
    val toolbar: MangaToolbarActions,
    val header: MangaHeaderActions,
    val info: MangaInfoActions,
    val chapters: ChapterRowActions,
    val selection: ChapterSelectionActions,
    // SY -->
    val sy: SyMangaActions,
    // SY <--
)
