package eu.kanade.tachiyomi.ui.library

import eu.kanade.core.util.fastFilterNot
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.chapter.interactor.GetBookmarkedChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Queueing chapter downloads for a library selection: the next N unread chapters or the
 * bookmarked ones, per entry, with merged entries fanned out to their parts. Composed by
 * [LibraryScreenModel].
 */
internal class LibraryDownloads(
    private val downloadManager: DownloadManager = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val getBookmarkedChaptersByMangaId: GetBookmarkedChaptersByMangaId = Injekt.get(),
    private val getMergedMangaById: GetMergedMangaById = Injekt.get(),
) {
    suspend fun downloadNextChapters(mangas: List<Manga>, amount: Int?) {
        mangas.forEach { manga ->
            val nextChapters = getNextChapters.await(manga.id)
            // SY -->
            if (manga.source == MERGED_SOURCE_ID) {
                downloadMergedParts(manga, nextChapters.limitTo(amount))
            } else {
                // SY <--
                val chapters = nextChapters.fastFilterNot { isQueuedOrDownloaded(it, manga) }.limitTo(amount)
                downloadManager.downloadChapters(manga, chapters)
            }
        }
    }

    suspend fun downloadBookmarkedChapters(mangas: List<Manga>) {
        mangas.forEach { manga ->
            val bookmarked = getBookmarkedChaptersByMangaId.await(manga.id)
            // SY -->
            if (manga.source == MERGED_SOURCE_ID) {
                downloadMergedParts(manga, bookmarked)
            } else {
                // SY <--
                downloadManager.downloadChapters(manga, bookmarked.fastFilterNot { isQueuedOrDownloaded(it, manga) })
            }
        }
    }

    // A merged entry's chapters belong to its parts; each part queues its own.
    private suspend fun downloadMergedParts(manga: Manga, chapters: List<Chapter>) {
        val mergedMangas = getMergedMangaById.await(manga.id).associateBy { it.id }
        chapters.groupBy { it.mangaId }.forEach { (mangaId, partChapters) ->
            mergedMangas[mangaId]?.let { part ->
                downloadManager.downloadChapters(part, partChapters.fastFilterNot { isQueuedOrDownloaded(it, part) })
            }
        }
    }

    private fun isQueuedOrDownloaded(chapter: Chapter, manga: Manga): Boolean =
        downloadManager.getQueuedDownloadOrNull(chapter.id) != null ||
            downloadManager.isChapterDownloaded(
                chapter.name,
                chapter.scanlator,
                chapter.url,
                // SY -->
                manga.ogTitle,
                // SY <--
                manga.source,
            )
}

internal fun List<Chapter>.limitTo(amount: Int?): List<Chapter> = if (amount != null) take(amount) else this
