package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.domain.manga.model.downloadedFilter
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.flow.filter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.calculateChapterGap
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.manga.model.applyFilter
import tachiyomi.domain.manga.model.bookmarkedFilter
import tachiyomi.domain.manga.model.sortDescending
import tachiyomi.domain.manga.model.unreadFilter
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.api.get
import kotlin.math.floor

// Applies the view filters to the list of chapters obtained from the database.
// @return the chapters filtered and sorted.
internal fun List<ChapterList.Item>.applyFilters(manga: Manga): Sequence<ChapterList.Item> {
    val isLocalManga = manga.isLocal()
    val unreadFilter = manga.unreadFilter
    val downloadedFilter = manga.downloadedFilter
    val bookmarkedFilter = manga.bookmarkedFilter
    return asSequence()
        .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalManga } }
        .sortedWith { (chapter1), (chapter2) -> getChapterSort(manga).invoke(chapter1, chapter2) }
}

internal data class MergedMangaData(
    val references: List<MergedMangaReference>,
    val manga: Map<Long, Manga>,
    val sources: List<Source>,
)

@Immutable
internal sealed class ChapterList {
    @Immutable
    data class MissingCount(
        val id: String,
        val count: Int,
    ) : ChapterList()

    @Immutable
    data class Item(
        val chapter: Chapter,
        val downloadState: Download.State,
        val downloadProgress: Int,
        val selected: Boolean = false,
        // SY -->
        val sourceName: String?,
        val showScanlator: Boolean,
        // SY <--
    ) : ChapterList() {
        val id = chapter.id
        val isDownloaded = downloadState == Download.State.DOWNLOADED
    }
}

// SY -->
internal sealed interface PagePreviewState {
    data object Unused : PagePreviewState
    data object Loading : PagePreviewState
    data class Success(val pagePreviews: List<PagePreview>) : PagePreviewState
    data class Error(val error: Throwable) : PagePreviewState
}
// SY <--

// The chapter list with a MissingCount separator wherever chapter numbers skip.
internal fun List<ChapterList.Item>.insertMissingCounts(manga: Manga): List<ChapterList> =
    insertSeparators { before, after ->
        val (lowerChapter, higherChapter) = if (manga.sortDescending()) {
            after to before
        } else {
            before to after
        }
        higherChapter?.let { higher ->
            val gap = if (lowerChapter == null) {
                floor(higher.chapter.chapterNumber).toInt().minus(1).coerceAtLeast(0)
            } else {
                calculateChapterGap(higher.chapter, lowerChapter.chapter)
            }
            gap.takeIf { it > 0 }?.let { missingCount ->
                ChapterList.MissingCount(id = "${lowerChapter?.id}-${higher.id}", count = missingCount)
            }
        }
    }
