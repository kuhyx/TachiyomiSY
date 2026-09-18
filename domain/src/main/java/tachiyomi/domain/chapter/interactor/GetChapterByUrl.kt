package tachiyomi.domain.chapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository

/** Looks up chapters by url across all manga. */
public class GetChapterByUrl(
    private val chapterRepository: ChapterRepository,
) {

    /** Every chapter at [url]; logs and returns an empty list when the store fails. */
    public suspend fun await(url: String): List<Chapter> {
        return try {
            chapterRepository.getChapterByUrl(url)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            emptyList()
        }
    }
}
