package tachiyomi.domain.manga.interactor

import exh.metadata.sql.models.SearchTitle
import tachiyomi.domain.manga.repository.MangaMetadataRepository

/** Reads the alternative titles stored with a manga's search metadata. */
public class GetSearchTitles(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    /** Search titles of manga [mangaId]; empty when it has none. */
    public suspend fun await(mangaId: Long): List<SearchTitle> = mangaMetadataRepository.getTitlesById(mangaId)
}
