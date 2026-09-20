package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.cancelQueuedDownloads
import eu.kanade.tachiyomi.data.download.deleteChapters
import eu.kanade.tachiyomi.data.download.deleteManga
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.startDownloadNow
import eu.kanade.tachiyomi.data.download.startDownloads
import eu.kanade.tachiyomi.source.online.all.MergedSource
import exh.source.isEhBasedManga
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import logcat.LogPriority
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The download side of the manga screen: mirroring the download queue into the chapter list,
 * starting, cancelling and deleting chapter downloads, and the add-to-library prompt that the
 * first download of a non-library entry raises. Composed by [MangaScreenModel], whose state it
 * updates.
 */
internal class MangaDownloads(
    private val model: MangaScreenModel,
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val filterChaptersForDownload: FilterChaptersForDownload = Injekt.get(),
) {
    fun observe() {
        // SY -->
        val isMergedSource = model.source is MergedSource
        val mergedIds = if (isMergedSource) model.successState?.mergedData?.manga?.keys.orEmpty() else emptySet()
        // SY <--
        model.screenModelScope.launchIO {
            downloadManager.statusFlow()
                .filter {
                    // SY -->
                    if (isMergedSource) {
                        it.manga.id in mergedIds
                    } else {
                        // SY <--
                        it.manga.id == model.successState?.manga?.id
                    }
                }
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .flowWithLifecycle(lifecycle)
                .collect {
                    withUIContext {
                        updateDownloadState(it)
                    }
                }
        }

        model.screenModelScope.launchIO {
            downloadManager.progressFlow()
                .filter {
                    // SY -->
                    if (isMergedSource) {
                        it.manga.id in mergedIds
                    } else {
                        // SY <--
                        it.manga.id == model.successState?.manga?.id
                    }
                }
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .flowWithLifecycle(lifecycle)
                .collect {
                    withUIContext {
                        updateDownloadState(it)
                    }
                }
        }
    }

    private fun updateDownloadState(download: Download) {
        model.updateSuccessState { successState ->
            val modifiedIndex = successState.chapters.indexOfFirst { it.id == download.chapter.id }
            if (modifiedIndex < 0) {
                successState
            } else {
                val newChapters = successState.chapters.toMutableList().apply {
                    val item = removeAt(modifiedIndex)
                        .copy(downloadState = download.status, downloadProgress = download.progress)
                    add(modifiedIndex, item)
                }
                successState.copy(chapters = newChapters)
            }
        }
    }

    // Returns true if the manga has any downloads.
    fun hasDownloads(): Boolean {
        val manga = model.successState?.manga ?: return false
        return downloadManager.getDownloadCount(manga) > 0
    }

    // Deletes all the downloads for the manga.
    fun deleteDownloads() {
        val state = model.successState ?: return
        // SY -->
        if (state.source is MergedSource) {
            val mergedManga = state.mergedData?.manga?.map { it.value to sourceManager.getOrStub(it.value.source) }
            mergedManga?.forEach { (manga, source) ->
                downloadManager.deleteManga(manga, source)
            }
        } else {
            /* SY <-- */ downloadManager.deleteManga(state.manga, state.source)
        }
    }

    private fun startDownload(
        chapters: List<Chapter>,
        startNow: Boolean,
    ) {
        val successState = model.successState ?: return

        model.screenModelScope.launchNonCancellable {
            if (startNow) {
                val chapterId = chapters.singleOrNull()?.id ?: return@launchNonCancellable
                downloadManager.startDownloadNow(chapterId)
            } else {
                downloadChapters(chapters)
            }

            if (!model.isFavorited && !successState.hasPromptedToAddBefore) {
                model.updateSuccessState { state ->
                    state.copy(hasPromptedToAddBefore = true)
                }
                val result = model.snackbarHostState.showSnackbar(
                    message = context.stringResource(MR.strings.snack_add_to_library),
                    actionLabel = context.stringResource(MR.strings.action_add),
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed && !model.isFavorited) {
                    model.toggleFavorite()
                }
            }
        }
    }

    fun runChapterDownloadActions(
        items: List<ChapterList.Item>,
        action: ChapterDownloadAction,
    ) {
        when (action) {
            ChapterDownloadAction.START -> {
                startDownload(items.map { it.chapter }, false)
                if (items.any { it.downloadState == Download.State.ERROR }) {
                    downloadManager.startDownloads()
                }
            }
            ChapterDownloadAction.START_NOW -> {
                val chapter = items.singleOrNull()?.chapter ?: return
                startDownload(listOf(chapter), true)
            }
            ChapterDownloadAction.CANCEL -> {
                val chapterId = items.singleOrNull()?.id ?: return
                val activeDownload = downloadManager.getQueuedDownloadOrNull(chapterId) ?: return
                downloadManager.cancelQueuedDownloads(listOf(activeDownload))
                updateDownloadState(activeDownload.apply { transition(Download.State.NOT_DOWNLOADED) })
            }
            ChapterDownloadAction.DELETE -> {
                deleteChapters(items.map { it.chapter })
            }
        }
    }

    fun runDownloadAction(action: DownloadAction) {
        val chaptersToDownload = when (action) {
            DownloadAction.UNREAD_CHAPTERS -> model.getUnreadChapters()
            DownloadAction.BOOKMARKED_CHAPTERS -> model.getBookmarkedChapters()
            else -> model.getUnreadChaptersSorted().take(checkNotNull(action.nextChapters))
        }
        if (chaptersToDownload.isNotEmpty()) {
            startDownload(chaptersToDownload, false)
        }
    }

    // Downloads the given list of chapters with the manager.
    // @param chapters the list of chapters to download.
    private fun downloadChapters(chapters: List<Chapter>) {
        val state = model.successState ?: return
        if (state.source is MergedSource) {
            chapters.groupBy { it.mangaId }.forEach { map ->
                val manga = state.mergedData?.manga?.get(map.key)
                if (manga != null) {
                    downloadManager.downloadChapters(manga, map.value)
                }
            }
        } else {
            /* SY <-- */
            val manga = state.manga
            downloadManager.downloadChapters(manga, chapters)
        }
        model.toggleAllSelection(false)
    }

    /**
     * Deletes the given list of chapter.
     *
     * @param chapters the list of chapters to delete.
     */
    fun deleteChapters(chapters: List<Chapter>) {
        model.screenModelScope.launchNonCancellable {
            try {
                model.successState?.let { state ->
                    downloadManager.deleteChapters(
                        chapters,
                        state.manga,
                        state.source,
                    )
                }
            } catch (expected: Throwable) {
                // Logged whatever the cause; the caller carries on.
                logcat(LogPriority.ERROR, expected)
            }
        }
    }

    fun downloadNewChapters(chapters: List<Chapter>) {
        model.screenModelScope.launchNonCancellable {
            val manga = model.successState?.manga
            if (manga != null) {
                val chaptersToDownload = filterChaptersForDownload.await(manga, chapters)

                if (chaptersToDownload.isNotEmpty() /* SY --> */ && !manga.isEhBasedManga() /* SY <-- */) {
                    downloadChapters(chaptersToDownload)
                }
            }
        }
    }
}
