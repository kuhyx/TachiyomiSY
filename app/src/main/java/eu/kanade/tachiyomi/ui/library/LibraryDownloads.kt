package eu.kanade.tachiyomi.ui.library

import androidx.compose.ui.util.fastAny
import eu.kanade.core.util.fastFilterNot
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.chapter.interactor.GetBookmarkedChaptersByMangaId
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
            // SY -->
            if (manga.source == MERGED_SOURCE_ID) {
                val mergedMangas = getMergedMangaById.await(manga.id)
                    .associateBy { it.id }
                getNextChapters.await(manga.id)
                    .let { if (amount != null) it.take(amount) else it }
                    .groupBy { it.mangaId }
                    .forEach { (mangaId, chapters) ->
                        val mergedManga = mergedMangas[mangaId]
                        if (mergedManga != null) {
                            val downloadChapters = chapters.fastFilterNot { chapter ->
                                downloadManager.queueState.value.fastAny { chapter.id == it.chapter.id } ||
                                    downloadManager.isChapterDownloaded(
                                        chapter.name,
                                        chapter.scanlator,
                                        chapter.url,
                                        mergedManga.ogTitle,
                                        mergedManga.source,
                                    )
                            }

                            downloadManager.downloadChapters(mergedManga, downloadChapters)
                        }
                    }
            } else {
                // SY <--

                val chapters = getNextChapters.await(manga.id)
                    .fastFilterNot { chapter ->
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
                    .let { if (amount != null) it.take(amount) else it }

                downloadManager.downloadChapters(manga, chapters)
            }
        }
    }

    suspend fun downloadBookmarkedChapters(mangas: List<Manga>) {
        mangas.forEach { manga ->
            // SY -->
            if (manga.source == MERGED_SOURCE_ID) {
                val mergedMangas = getMergedMangaById.await(manga.id)
                    .associateBy { it.id }
                getBookmarkedChaptersByMangaId.await(manga.id)
                    .groupBy { it.mangaId }
                    .forEach { (mangaId, chapters) ->
                        val mergedManga = mergedMangas[mangaId]
                        if (mergedManga != null) {
                            val downloadChapters = chapters.fastFilterNot { chapter ->
                                downloadManager.queueState.value.fastAny { chapter.id == it.chapter.id } ||
                                    downloadManager.isChapterDownloaded(
                                        chapter.name,
                                        chapter.scanlator,
                                        chapter.url,
                                        mergedManga.ogTitle,
                                        mergedManga.source,
                                    )
                            }

                            downloadManager.downloadChapters(mergedManga, downloadChapters)
                        }
                    }
            } else {
                // SY <--

                val chapters = getBookmarkedChaptersByMangaId.await(manga.id)
                    .fastFilterNot { chapter ->
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
                downloadManager.downloadChapters(manga, chapters)
            }
        }
    }
}
