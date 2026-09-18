package tachiyomi.domain.category.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.repository.MangaRepository

/** Replaces the set of categories a manga belongs to. */
public class SetMangaCategories(
    private val mangaRepository: MangaRepository,
) {

    /** Links the manga to exactly [categoryIds]; a store failure is logged and swallowed. */
    public suspend fun await(mangaId: Long, categoryIds: List<Long>) {
        try {
            mangaRepository.setMangaCategories(mangaId, categoryIds)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
        }
    }
}
