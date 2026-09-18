package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaMergeRepository

/** Lists the entries of a merged manga whose new chapters are downloaded automatically. */
public class GetMergedMangaForDownloading(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    /** The manga of merge [mergeId] whose chapters are downloaded. */
    public suspend fun await(mergeId: Long): List<Manga> = mangaMergeRepository.getMergeMangaForDownloading(mergeId)
}
