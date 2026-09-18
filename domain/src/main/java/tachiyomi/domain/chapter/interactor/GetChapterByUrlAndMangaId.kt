package tachiyomi.domain.chapter.interactor

import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository

/** Looks up a chapter by its url within one manga. */
public class GetChapterByUrlAndMangaId(
    private val chapterRepository: ChapterRepository,
) {

    /**
     * The chapter at [url] of the manga whose id is [sourceId]; null when there is none or
     * the store fails (not logged).
     */
    public suspend fun await(url: String, sourceId: Long): Chapter? {
        return try {
            chapterRepository.getChapterByUrlAndMangaId(url, sourceId)
        } catch (expected: Exception) {
            // Any failure of the store degrades to the fallback below.
            null
        }
    }
}
