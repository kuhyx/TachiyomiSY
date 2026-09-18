package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaMergeRepository

/** Lists every manga that is part of a merge. */
public class GetMergedManga(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    /** Every merged-in manga; logs and returns an empty list when the store fails. */
    public suspend fun await(): List<Manga> {
        return try {
            mangaMergeRepository.getMergedManga()
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            emptyList()
        }
    }

    /** [await] as a flow that re-emits on every change. */
    public suspend fun subscribe(): Flow<List<Manga>> = mangaMergeRepository.subscribeMergedManga()
}
