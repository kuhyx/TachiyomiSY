package tachiyomi.domain.manga.repository

import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga

/** The read half of [MangaMetadataRepository]: search metadata, tags and titles per manga. */
public interface MangaMetadataReadRepository {

    /** Search metadata of manga [id], or null. */
    public suspend fun getMetadataById(id: Long): SearchMetadata?

    /** [getMetadataById] as a flow. */
    public fun subscribeMetadataById(id: Long): Flow<SearchMetadata?>

    /** Search tags of manga [id]. */
    public suspend fun getTagsById(id: Long): List<SearchTag>

    /** [getTagsById] as a flow. */
    public fun subscribeTagsById(id: Long): Flow<List<SearchTag>>

    /** Search titles of manga [id]. */
    public suspend fun getTitlesById(id: Long): List<SearchTitle>

    /** [getTitlesById] as a flow. */
    public fun subscribeTitlesById(id: Long): Flow<List<SearchTitle>>

    /** Favourites from the E-Hentai sources that have metadata. */
    public suspend fun getExhFavoritesWithMetadata(): List<Manga>

    /** Ids of every favourite that has metadata. */
    public suspend fun getFavoriteIdsWithMetadata(): List<Long>

    /** Every search metadata row. */
    public suspend fun getSearchMetadata(): List<SearchMetadata>
}
