package tachiyomi.data.manga

import exh.metadata.metadata.base.FlatMetadata
import tachiyomi.data.Database
import tachiyomi.domain.manga.repository.MangaMetadataReadRepository
import tachiyomi.domain.manga.repository.MangaMetadataRepository

/** [MangaMetadataRepository] on the SQLDelight `search_*` tables (SY); reads are delegated. */
public class MangaMetadataRepositoryImpl(
    private val database: Database,
) : MangaMetadataRepository,
    MangaMetadataReadRepository by MangaMetadataReadRepositoryImpl(database) {

    override suspend fun insertFlatMetadata(flatMetadata: FlatMetadata) {
        require(flatMetadata.metadata.mangaId != -1L) { "Metadata must belong to a stored manga" }

        database.transaction {
            flatMetadata.metadata.run {
                database.search_metadataQueries.upsert(mangaId, uploader, extra, indexedExtra, extraVersion.toLong())
            }
            database.search_tagsQueries.deleteByManga(flatMetadata.metadata.mangaId)
            flatMetadata.tags.forEach {
                database.search_tagsQueries.insert(it.mangaId, it.namespace, it.name, it.type.toLong())
            }
            database.search_titlesQueries.deleteByManga(flatMetadata.metadata.mangaId)
            flatMetadata.titles.forEach {
                database.search_titlesQueries.insert(it.mangaId, it.title, it.type.toLong())
            }
        }
    }
}
