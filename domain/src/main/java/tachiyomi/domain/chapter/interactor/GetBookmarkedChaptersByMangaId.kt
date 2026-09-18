package tachiyomi.domain.chapter.interactor

import exh.source.MERGED_SOURCE_ID
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.interactor.GetManga

/** Lists the bookmarked chapters of one manga, merged manga included. */
public class GetBookmarkedChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
    // SY -->
    private val getManga: GetManga,
    private val getMergedChaptersByMangaId: GetMergedChaptersByMangaId,
    // SY <--
) {

    /** Bookmarked chapters of [mangaId]; empty when the manga is unknown or the store fails (logged). */
    public suspend fun await(mangaId: Long): List<Chapter> {
        return try {
            // SY -->
            val manga = getManga.await(mangaId)
            when {
                manga == null -> {
                    emptyList()
                }
                manga.source == MERGED_SOURCE_ID -> {
                    getMergedChaptersByMangaId.await(mangaId, applyScanlatorFilter = true).filter { it.bookmark }
                }
                else -> {
                    chapterRepository.getBookmarkedChaptersByMangaId(mangaId)
                }
            }
            // SY <--
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            emptyList()
        }
    }
}
