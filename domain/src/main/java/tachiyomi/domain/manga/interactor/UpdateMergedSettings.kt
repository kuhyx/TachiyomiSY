package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.MergeMangaSettingsUpdate
import tachiyomi.domain.manga.repository.MangaMergeRepository

/** Writes the per-entry settings of merged manga. */
public class UpdateMergedSettings(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    /** Applies [mergeUpdate]; true on success. */
    public suspend fun await(mergeUpdate: MergeMangaSettingsUpdate): Boolean =
        mangaMergeRepository.updateSettings(mergeUpdate)

    /** Applies every update in [values] in one transaction; true on success. */
    public suspend fun awaitAll(values: List<MergeMangaSettingsUpdate>): Boolean =
        mangaMergeRepository.updateAllSettings(values)
}
