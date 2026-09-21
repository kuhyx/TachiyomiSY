package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.State
import eu.kanade.tachiyomi.ui.reader.setting.preserveReadingPosition
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.metadata.base.raise
import exh.source.MERGED_SOURCE_ID
import exh.source.getMainSource
import exh.source.isEhBasedManga
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import uy.kohesive.injekt.api.get

internal fun MangaScreenModel.loadInitialState() {
    screenModelScope.launchIO {
        val manga = getMangaAndChapters.awaitManga(mangaId)
        // SY -->
        val mergedData = awaitMergedData()
        val rawChapters = if (manga.source == MERGED_SOURCE_ID) {
            getMergedChaptersByMangaId.await(mangaId, applyScanlatorFilter = true)
        } else {
            getMangaAndChapters.awaitChapters(mangaId, applyScanlatorFilter = true)
        }
        val chapters = toChapterListItems(rawChapters, manga, mergedData)
        val meta = getFlatMetadata.await(mangaId)
        // SY <--

        if (!manga.favorite) {
            setMangaDefaultChapterFlags.await(manga)
        }

        val needRefreshInfo = !manga.initialized
        val needRefreshChapter = chapters.isEmpty()

        // Show what we have earlier
        val initial = initialSuccessState(manga, chapters, mergedData, meta, needRefreshInfo || needRefreshChapter)
        updateState { initial }

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

// SY -->
private suspend fun MangaScreenModel.awaitMergedData(): MergedMangaData? =
    getMergedReferencesById.await(mangaId).takeIf { it.isNotEmpty() }?.let { references ->
        MergedMangaData(
            references,
            getMergedMangaById.await(mangaId).associateBy { it.id },
            references.map { it.mangaSourceId }.distinct()
                .map { sourceManager.getOrStub(it) },
        )
    }
// SY <--

private suspend fun MangaScreenModel.initialSuccessState(
    manga: Manga,
    chapters: List<ChapterList.Item>,
    mergedData: MergedMangaData?,
    meta: FlatMetadata?,
    isRefreshingData: Boolean,
): State.Success {
    val source = sourceManager.getOrStub(manga.source)
    return State.Success(
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
        isRefreshingData = isRefreshingData,
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

internal fun MangaScreenModel.fetchAllFromSource(manualFetch: Boolean = true) {
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

internal suspend fun MangaScreenModel.fetchAllFromSource(
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
internal fun MangaScreenModel.raiseMetadata(flatMetadata: FlatMetadata?, source: Source): RaisedSearchMetadata? {
    val metaClass = source.getMainSource<MetadataSource<*, *>>()?.metaClass ?: return null
    return flatMetadata?.raise(metaClass)
}
