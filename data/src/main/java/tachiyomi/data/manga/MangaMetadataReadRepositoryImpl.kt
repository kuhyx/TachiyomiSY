package tachiyomi.data.manga

import app.cash.sqldelight.async.coroutines.awaitAsList
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.awaitOneOrNull
import tachiyomi.data.subscribeToList
import tachiyomi.data.subscribeToOneOrNull
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaMetadataReadRepository

/** [MangaMetadataReadRepository] on the SQLDelight `search_*` tables (SY). */
internal class MangaMetadataReadRepositoryImpl(
    private val database: Database,
) : MangaMetadataReadRepository {

    override suspend fun getMetadataById(id: Long): SearchMetadata? {
        return database.search_metadataQueries
            .selectByMangaId(id)
            .awaitOneOrNull(MangaMetadataMapper::mapMetadata)
    }

    override fun subscribeMetadataById(id: Long): Flow<SearchMetadata?> {
        return database.search_metadataQueries
            .selectByMangaId(id)
            .subscribeToOneOrNull(MangaMetadataMapper::mapMetadata)
    }

    override suspend fun getTagsById(id: Long): List<SearchTag> {
        return database.search_tagsQueries
            .selectByMangaId(id)
            .awaitList(MangaMetadataMapper::mapTag)
    }

    override fun subscribeTagsById(id: Long): Flow<List<SearchTag>> {
        return database.search_tagsQueries
            .selectByMangaId(id)
            .subscribeToList(MangaMetadataMapper::mapTag)
    }

    override suspend fun getTitlesById(id: Long): List<SearchTitle> {
        return database.search_titlesQueries
            .selectByMangaId(id)
            .awaitList(MangaMetadataMapper::mapTitle)
    }

    override fun subscribeTitlesById(id: Long): Flow<List<SearchTitle>> {
        return database.search_titlesQueries
            .selectByMangaId(id)
            .subscribeToList(MangaMetadataMapper::mapTitle)
    }

    override suspend fun getExhFavoritesWithMetadata(): List<Manga> {
        return database.mangasQueries
            .getEhMangaWithMetadata(EH_SOURCE_ID, EXH_SOURCE_ID)
            .awaitList(MangaMapper::mapManga)
    }

    override suspend fun getFavoriteIdsWithMetadata(): List<Long> {
        return database.mangasQueries
            .getIdsOfFavoriteMangaWithMetadata()
            .awaitAsList()
    }

    override suspend fun getSearchMetadata(): List<SearchMetadata> {
        return database.search_metadataQueries
            .selectAll()
            .awaitList(MangaMetadataMapper::mapMetadata)
    }
}
