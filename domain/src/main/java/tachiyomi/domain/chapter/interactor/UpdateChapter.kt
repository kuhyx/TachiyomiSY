package tachiyomi.domain.chapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository

/** Writes partial chapter updates to the store. */
public class UpdateChapter(
    private val chapterRepository: ChapterRepository,
) {

    /** Applies [chapterUpdate]; a failing store is logged and otherwise ignored. */
    public suspend fun await(chapterUpdate: ChapterUpdate) {
        try {
            chapterRepository.update(chapterUpdate)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
        }
    }

    /**
     * Applies every update in [chapterUpdates] in one transaction; a failing store is logged and
     * otherwise ignored.
     */
    public suspend fun awaitAll(chapterUpdates: List<ChapterUpdate>) {
        try {
            chapterRepository.updateAll(chapterUpdates)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
        }
    }
}
