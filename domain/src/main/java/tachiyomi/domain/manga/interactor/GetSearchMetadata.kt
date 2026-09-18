package tachiyomi.domain.manga.interactor

import exh.metadata.sql.models.SearchMetadata
import tachiyomi.domain.manga.repository.MangaMetadataRepository

/** Reads the search metadata that metadata-aware sources attach to manga. */
public class GetSearchMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    /** The search metadata of manga [mangaId], or null when none is stored. */
    public suspend fun await(mangaId: Long): SearchMetadata? = mangaMetadataRepository.getMetadataById(mangaId)

    /** The search metadata of every manga that has any. */
    public suspend fun await(): List<SearchMetadata> = mangaMetadataRepository.getSearchMetadata()
}
