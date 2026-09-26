package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.getValue
import androidx.lifecycle.flowWithLifecycle
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.CombineState
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.EXHRedirect
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.State
import exh.debug.DebugToggles
import exh.log.xLogD
import exh.source.MERGED_SOURCE_ID
import exh.source.isEhBasedManga
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

internal fun MangaScreenModel.observeMangaAndChapters() {
    screenModelScope.launchIO {
        mangaAndChaptersFlow()
            // SY -->
            .onEach { (manga, chapters) -> redirectToAcceptedRoot(manga, chapters) }
            .combine(getFlatMetadata.subscribe(mangaId).distinctUntilChanged()) { pair, flatMetadata ->
                CombineState(pair, flatMetadata)
            }
            .combine(mergedDataFlow()) { state, mergedData -> state.copy(mergedData = mergedData) }
            .combine(downloadCache.changes) { state, _ -> state }
            .combine(downloadManager.queueState) { state, _ -> state }
            // SY <--
            .flowWithLifecycle(lifecycle)
            .collectLatest { combined ->
                val manga = combined.manga
                val mergedData = combined.mergedData
                val chapterItems = toChapterListItems(combined.chapters, manga /* SY --> */, mergedData /* SY <-- */)
                // The items were built outside the atomic update (download checks do disk IO), so a
                // selection toggled meanwhile is re-applied here instead of being overwritten.
                updateSuccessState {
                    it.copy(
                        manga = manga,
                        chapters = selection.reapply(chapterItems),
                        // SY -->
                        meta = raiseMetadata(combined.flatMetadata, it.source),
                        mergedData = mergedData,
                        // SY <--
                    )
                }
            }
    }
}

// SY -->
// The manga with its own chapters, or with the merged chapters when it is a merged manga.
private suspend fun MangaScreenModel.mangaAndChaptersFlow(): Flow<Pair<Manga, List<Chapter>>> =
    getMangaAndChapters.subscribe(mangaId, applyScanlatorFilter = true)
        .distinctUntilChanged()
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

private suspend fun MangaScreenModel.mergedDataFlow(): Flow<MergedMangaData?> = combine(
    getMergedMangaById.subscribe(mangaId).distinctUntilChanged(),
    getMergedReferencesById.subscribe(mangaId).distinctUntilChanged(),
) { manga, references ->
    if (manga.isNotEmpty()) {
        MergedMangaData(
            references,
            manga.associateBy { it.id },
            references.map { it.mangaSourceId }.distinct().map { sourceManager.getOrStub(it) },
        )
    } else {
        null
    }
}

// Check for gallery in library and accept manga with lowest id: find chapters sharing the same
// root and redirect when this manga is not the accepted one.
private fun MangaScreenModel.redirectToAcceptedRoot(manga: Manga, chapters: List<Chapter>) {
    if (chapters.isEmpty() || !manga.isEhBasedManga() || !DebugToggles.ENABLE_EXH_ROOT_REDIRECT.enabled) return
    screenModelScope.launchIO {
        try {
            val (acceptedChain) = updateHelper.acceptRootAndDiscardOthers(manga.source, chapters)
            // Redirect if we are not the accepted root
            if (manga.id != acceptedChain.manga.id && acceptedChain.manga.favorite) {
                // Update if any of our chapters are not in accepted manga's chapters
                xLogD("Found accepted manga %s", manga.url)
                redirectFlow.emit(EXHRedirect(acceptedChain.manga.id))
            }
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected) { "Error loading accepted chapter chain" }
        }
    }
}
// SY <--

internal fun MangaScreenModel.observeExcludedScanlators() {
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
}

internal fun MangaScreenModel.observeAvailableScanlators() {
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
}
