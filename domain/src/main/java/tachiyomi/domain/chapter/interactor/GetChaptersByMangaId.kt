package tachiyomi.domain.chapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository

/** Lists the chapters of one manga. */
public class GetChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
) {

    /**
     * Chapters of [mangaId]; [applyScanlatorFilter] drops the manga's excluded scanlators.
     * Logs and returns an empty list when the store fails.
     */
    public suspend fun await(mangaId: Long, applyScanlatorFilter: Boolean = false): List<Chapter> {
        return try {
            chapterRepository.getChapterByMangaId(mangaId, applyScanlatorFilter)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            emptyList()
        }
    }
}
