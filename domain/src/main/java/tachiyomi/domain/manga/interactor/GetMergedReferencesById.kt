package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.manga.repository.MangaMergeRepository

/** Lists the entries ([MergedMangaReference]) that make up one merged manga. */
public class GetMergedReferencesById(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    /** The references of merge [id]; logs and returns an empty list when the store fails. */
    public suspend fun await(id: Long): List<MergedMangaReference> {
        return try {
            mangaMergeRepository.getReferencesById(id)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            emptyList()
        }
    }

    /** [await] as a flow that re-emits on every change. */
    public suspend fun subscribe(id: Long): Flow<List<MergedMangaReference>> =
        mangaMergeRepository.subscribeReferencesById(id)
}
