package tachiyomi.domain.chapter.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.interactor.GetMergedReferencesById

/** Lists the chapters of a merged manga, collected from every entry merged into it. */
public class GetMergedChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
    private val getMergedReferencesById: GetMergedReferencesById,
) {

    /**
     * Chapters of merge [mangaId]; [dedupe] applies the merge's dedupe mode and [applyScanlatorFilter]
     * drops excluded scanlators. A failing store is logged and contributes no chapters.
     */
    public suspend fun await(
        mangaId: Long,
        dedupe: Boolean = true,
        applyScanlatorFilter: Boolean = false,
    ): List<Chapter> {
        return MergedChapterDedupe.apply(
            getMergedReferencesById.await(mangaId),
            getFromDatabase(mangaId, applyScanlatorFilter),
            dedupe,
        )
    }

    /** [await] as a flow that re-emits on every change; logs and emits an empty list when the store fails. */
    public suspend fun subscribe(
        mangaId: Long,
        dedupe: Boolean = true,
        applyScanlatorFilter: Boolean = false,
    ): Flow<List<Chapter>> {
        return try {
            chapterRepository.getMergedChapterByMangaIdFlow(mangaId, applyScanlatorFilter)
                .combine(getMergedReferencesById.subscribe(mangaId)) { chapters, references ->
                    MergedChapterDedupe.apply(references, chapters, dedupe)
                }
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            flowOf(emptyList())
        }
    }

    private suspend fun getFromDatabase(
        mangaId: Long,
        applyScanlatorFilter: Boolean,
    ): List<Chapter> {
        return try {
            chapterRepository.getMergedChapterByMangaId(mangaId, applyScanlatorFilter)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            emptyList()
        }
    }
}
