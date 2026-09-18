package tachiyomi.domain.chapter.interactor

import tachiyomi.domain.chapter.repository.ChapterRepository

/** Deletes chapter rows by id. */
public class DeleteChapters(
    private val chapterRepository: ChapterRepository,
) {

    /** Deletes the chapters whose ids are [chapters]. */
    public suspend fun await(chapters: List<Long>) {
        chapterRepository.removeChaptersWithIds(chapters)
    }
}
