package eu.kanade.presentation.manga

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import eu.kanade.presentation.manga.components.ExpandableMangaDescription
import eu.kanade.presentation.manga.components.MangaActionRow
import eu.kanade.presentation.manga.components.MangaInfoBox
import eu.kanade.presentation.manga.components.MangaInfoButtons
import eu.kanade.presentation.manga.components.SearchMetadataChips
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.Lanraragi
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.source.online.all.NHentai
import eu.kanade.tachiyomi.source.online.english.EightMuses
import eu.kanade.tachiyomi.source.online.english.HBrowse
import eu.kanade.tachiyomi.source.online.english.Pururin
import eu.kanade.tachiyomi.source.online.english.Tsumino
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import eu.kanade.tachiyomi.util.system.copyToClipboard
import exh.source.getMainSource
import exh.ui.metadata.adapters.EHentaiDescription
import exh.ui.metadata.adapters.EightMusesDescription
import exh.ui.metadata.adapters.HBrowseDescription
import exh.ui.metadata.adapters.LanraragiDescription
import exh.ui.metadata.adapters.MangaDexDescription
import exh.ui.metadata.adapters.NHentaiDescription
import exh.ui.metadata.adapters.PururinDescription
import exh.ui.metadata.adapters.TsuminoDescription
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.StubSource
import java.time.Instant

@Composable
internal fun MangaScreen(
    state: MangaScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    isTabletUi: Boolean,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    actions: MangaScreenActions,
) {
    val layout = MangaScreenLayout(
        state = state,
        snackbarHostState = snackbarHostState,
        nextUpdate = nextUpdate,
        chapterSwipeStartAction = chapterSwipeStartAction,
        chapterSwipeEndAction = chapterSwipeEndAction,
        actions = actions,
    )
    if (!isTabletUi) {
        MangaScreenSmallImpl(layout)
    } else {
        MangaScreenLargeImpl(layout)
    }
}

/** What both layouts render from; the tags/notes copy helper is derived here once. */
internal data class MangaScreenLayout(
    val state: MangaScreenModel.State.Success,
    val snackbarHostState: SnackbarHostState,
    val nextUpdate: Instant?,
    val chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    val chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    val actions: MangaScreenActions,
) {
    val chapters: List<ChapterList.Item> get() = state.processedChapters
    val listItems: List<ChapterList> get() = state.chapterListItems
    val isAnySelected: Boolean get() = state.isAnySelected
}

// The info column shared by both layouts: cover box, action row, SY metadata, description, SY buttons.
// The phone layout lists these as separate lazy items; the tablet layout stacks them in a column.
@Composable
internal fun MangaInfoColumn(
    layout: MangaScreenLayout,
    appBarPadding: Dp,
    metadataDescription: MetadataDescriptionComposable?,
) {
    InfoBoxSection(layout, isTabletUi = true, appBarPadding = appBarPadding)
    ActionRowSection(layout)
    // SY -->
    MetadataSection(layout, metadataDescription)
    // SY <--
    DescriptionSection(layout, defaultExpandState = true)
    // SY -->
    InfoButtonsSection(layout)
    // SY <--
}

@Composable
internal fun InfoBoxSection(layout: MangaScreenLayout, isTabletUi: Boolean, appBarPadding: Dp) {
    val state = layout.state
    MangaInfoBox(
        isTabletUi = isTabletUi,
        appBarPadding = appBarPadding,
        manga = state.manga,
        sourceName = remember { state.source.getNameForMangaInfo(state.mergedData?.sources) },
        isStubSource = remember { state.source is StubSource },
        onCoverClick = layout.actions.info.onCoverClicked,
        doSearch = layout.actions.info.onSearch,
    )
}

@Composable
internal fun ActionRowSection(layout: MangaScreenLayout) {
    val state = layout.state
    val header = layout.actions.header
    MangaActionRow(
        favorite = state.manga.favorite,
        trackingCount = state.trackingCount,
        nextUpdate = layout.nextUpdate,
        isUserIntervalMode = state.manga.fetchInterval < 0,
        onAddToLibraryClicked = header.onAddToLibraryClicked,
        onWebViewClicked = header.onWebViewClicked,
        onWebViewLongClicked = header.onWebViewLongClicked,
        onTrackingClicked = header.onTrackingClicked,
        onEditIntervalClicked = header.onEditFetchIntervalClicked,
        onEditCategory = header.onEditCategoryClicked,
        // SY -->
        onMergeClicked = layout.actions.sy.merge.onMergeClicked.takeUnless { state.showMergeInOverflow },
        // SY <--
    )
}

// SY -->
@Composable
internal fun MetadataSection(layout: MangaScreenLayout, metadataDescription: MetadataDescriptionComposable?) {
    metadataDescription?.invoke(layout.state, layout.actions.sy.onMetadataViewerClicked) {
        layout.actions.info.onSearch(it, false)
    }
}
// SY <--

@Composable
internal fun DescriptionSection(layout: MangaScreenLayout, defaultExpandState: Boolean) {
    val state = layout.state
    val context = LocalContext.current
    ExpandableMangaDescription(
        defaultExpandState = defaultExpandState,
        description = state.manga.description,
        tagsProvider = { state.manga.genre },
        notes = state.manga.notes,
        onTagSearch = layout.actions.info.onTagSearch,
        onCopyTagToClipboard = { if (it.isNotEmpty()) context.copyToClipboard(it, it) },
        onEditNotes = layout.actions.info.onEditNotesClicked,
        // SY -->
        doSearch = layout.actions.info.onSearch,
        searchMetadataChips = remember(state.meta, state.source.id, state.manga.genre) {
            SearchMetadataChips(state.meta, state.source.id, state.manga.genre)
        },
        // SY <--
    )
}

// SY -->
@Composable
internal fun InfoButtonsSection(layout: MangaScreenLayout) {
    val state = layout.state
    if (!state.showRecommendationsInOverflow || state.showMergeWithAnother) {
        MangaInfoButtons(
            showRecommendsButton = !state.showRecommendationsInOverflow,
            showMergeWithAnotherButton = state.showMergeWithAnother,
            onRecommendClicked = layout.actions.sy.onRecommendClicked,
            onMergeWithAnotherClicked = layout.actions.sy.merge.onMergeWithAnotherClicked,
        )
    }
}
// SY <--

internal typealias MetadataDescriptionComposable = @Composable (
    state: MangaScreenModel.State.Success,
    openMetadataViewer: () -> Unit,
    search: (String) -> Unit,
) -> Unit

@Composable
internal fun metadataDescription(source: Source): MetadataDescriptionComposable? {
    val metadataSource = remember(source.id) { source.getMainSource<MetadataSource<*, *>>() }
    return remember(metadataSource) {
        when (metadataSource) {
            is EHentai -> { state, openMetadataViewer, search ->
                EHentaiDescription(state, openMetadataViewer, search)
            }
            is MangaDex -> { state, openMetadataViewer, _ ->
                MangaDexDescription(state, openMetadataViewer)
            }
            is NHentai -> { state, openMetadataViewer, _ ->
                NHentaiDescription(state, openMetadataViewer)
            }
            is EightMuses -> { state, openMetadataViewer, _ ->
                EightMusesDescription(state, openMetadataViewer)
            }
            is HBrowse -> { state, openMetadataViewer, _ ->
                HBrowseDescription(state, openMetadataViewer)
            }
            is Pururin -> { state, openMetadataViewer, _ ->
                PururinDescription(state, openMetadataViewer)
            }
            is Tsumino -> { state, openMetadataViewer, _ ->
                TsuminoDescription(state, openMetadataViewer)
            }
            is Lanraragi -> { state, openMetadataViewer, _ ->
                LanraragiDescription(state, openMetadataViewer)
            }
            else -> null
        }
    }
}
// SY <--
