package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.manga.interactor.GetPagePreviews
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import exh.source.isEhBasedManga
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal fun MangaScreenModel.toChapterListItems(
    chapters: List<Chapter>,
    manga: Manga,
    mergedData: MergedMangaData?,
): List<ChapterList.Item> {
    val isLocal = manga.isLocal()
    // SY -->
    val isExhManga = manga.isEhBasedManga()
    val enabledLanguages = Injekt.get<SourcePreferences>().enabledLanguages.get()
        .filterNot { it in listOf("all", "other") }
    // SY <--
    return chapters.map { chapter ->
        val activeDownload = if (isLocal) {
            null
        } else {
            downloadManager.getQueuedDownloadOrNull(chapter.id)
        }

        // SY -->
        @Suppress("NAME_SHADOWING")
        val manga = mergedData?.manga?.get(chapter.mangaId) ?: manga
        val source = mergedData?.sources?.find { manga.source == it.id }?.takeIf { mergedData.sources.size > 2 }
        // SY <--
        val downloaded = if (manga.isLocal()) {
            true
        } else {
            downloadManager.isChapterDownloaded(
                chapter.name,
                chapter.scanlator,
                chapter.url,
                /* SY --> */ manga.ogTitle, /* <-- SY */
                manga.source,
            )
        }
        val downloadState = when {
            activeDownload != null -> activeDownload.status
            downloaded -> Download.State.DOWNLOADED
            else -> Download.State.NOT_DOWNLOADED
        }

        ChapterList.Item(
            chapter = chapter,
            downloadState = downloadState,
            downloadProgress = activeDownload?.progress ?: 0,
            selected = chapter.id in selection.selectedChapterIds,
            // SY -->
            sourceName = source?.getNameForMangaInfo(enabledLanguages = enabledLanguages),
            showScanlator = !isExhManga,
            // SY <--
        )
    }
}

// SY -->
internal fun MangaScreenModel.getPagePreviews(manga: Manga, source: Source) {
    screenModelScope.launchIO {
        when (val result = getPagePreviews.await(manga, source, 1)) {
            is GetPagePreviews.Result.Error -> updateSuccessState {
                it.copy(pagePreviewsState = PagePreviewState.Error(result.error))
            }
            is GetPagePreviews.Result.Success -> updateSuccessState {
                it.copy(pagePreviewsState = PagePreviewState.Success(result.pagePreviews))
            }
            GetPagePreviews.Result.Unused -> updateSuccessState {
                it.copy(pagePreviewsState = PagePreviewState.Unused)
            }
        }
    }
}
