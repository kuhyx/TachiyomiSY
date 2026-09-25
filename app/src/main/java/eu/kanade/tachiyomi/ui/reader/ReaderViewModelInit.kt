package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.setting.autoscrollInterval
import exh.metadata.metadata.base.raise
import exh.source.getMainSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val MAX_PAGE_INPUT = 9999

internal fun ReaderViewModel.observeCurrentChapter() {
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
}

// SY -->
internal fun ReaderViewModel.observeAutoscrollFrequency() {
    state.mapLatest { it.ehAutoscrollFreq }
        .distinctUntilChanged()
        .drop(1)
        .onEach { text ->
            val parsed = text.toDoubleOrNull()

            if (parsed == null || parsed <= 0 || parsed > MAX_PAGE_INPUT) {
                readerPreferences.autoscrollInterval.set(-1f)
                updateState { it.copy(isAutoScrollEnabled = false) }
            } else {
                readerPreferences.autoscrollInterval.set(parsed.toFloat())
                updateState { it.copy(isAutoScrollEnabled = true) }
            }
        }
        .launchIn(viewModelScope)
}

/**
 * Initializes this presenter with the given [mangaId] and [initialChapterId]. This method will
 * fetch the manga from the database and initialize the initial chapter.
 */
internal suspend fun ReaderViewModel.init(
    mangaId: Long,
    initialChapterId: Long /* SY --> */,
    page: Int?/* SY <-- */,
): Result<Boolean> {
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
                    // Reader chapters come from the database, so every id is set.
                    chapterList.first { chapterId == it.chapter.id!! },
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

// The merged-source references and entries backing [manga], empty for any other source.
internal suspend fun ReaderViewModel.mergedDataFor(manga: Manga, source: Source): ChapterLoader.MergedData {
    if (source !is MergedSource) return ChapterLoader.MergedData(emptyList(), emptyMap())
    val references = getMergedReferencesById.await(manga.id)
    val mergedManga = getMergedMangaById.await(manga.id).associateBy { it.id }
    return ChapterLoader.MergedData(references, mergedManga)
}

internal suspend fun ReaderViewModel.publishInitialState(
    manga: Manga,
    source: Source,
    merged: ChapterLoader.MergedData,
) {
    val metadataSource = source.getMainSource<MetadataSource<*, *>>()
    val metadata = metadataSource?.let { getFlatMetadataById.await(manga.id)?.raise(it.metaClass) }
    val relativeTime = uiPreferences.relativeTime.get()
    val autoScrollFreq = readerPreferences.autoscrollInterval.get()
    updateState {
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
