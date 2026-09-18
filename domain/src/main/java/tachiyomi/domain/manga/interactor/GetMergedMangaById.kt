package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaMergeRepository

/** Lists the source manga merged into one merged manga. */
public class GetMergedMangaById(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    /** The manga merged into merge [id]; logs and returns an empty list when the store fails. */
    public suspend fun await(id: Long): List<Manga> {
        return try {
            mangaMergeRepository.getMergedMangaById(id)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            emptyList()
        }
    }

    /** [await] as a flow that re-emits on every change. */
    public suspend fun subscribe(id: Long): Flow<List<Manga>> = mangaMergeRepository.subscribeMergedMangaById(id)
}
