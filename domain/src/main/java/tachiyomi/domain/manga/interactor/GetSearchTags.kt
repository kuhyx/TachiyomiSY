package tachiyomi.domain.manga.interactor

import exh.metadata.sql.models.SearchTag
import tachiyomi.domain.manga.repository.MangaMetadataRepository

/** Reads the tags stored with a manga's search metadata. */
public class GetSearchTags(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    /** Search tags of manga [mangaId]; empty when it has none. */
    public suspend fun await(mangaId: Long): List<SearchTag> = mangaMetadataRepository.getTagsById(mangaId)
}
