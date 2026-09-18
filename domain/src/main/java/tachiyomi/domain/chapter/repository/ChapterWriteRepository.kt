package tachiyomi.domain.chapter.repository

import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate

/** The write half of [ChapterRepository]. */
public interface ChapterWriteRepository {

    /** Inserts [chapters] and returns them with their new ids. */
    public suspend fun addAll(chapters: List<Chapter>): List<Chapter>

    /** Applies a partial [chapterUpdate]. */
    public suspend fun update(chapterUpdate: ChapterUpdate)

    /** Applies every update in [chapterUpdates] in one transaction. */
    public suspend fun updateAll(chapterUpdates: List<ChapterUpdate>)

    /** Deletes the chapters with [chapterIds]. */
    public suspend fun removeChaptersWithIds(chapterIds: List<Long>)
}
