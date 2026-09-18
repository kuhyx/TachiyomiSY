package tachiyomi.data.manga

import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import tachiyomi.data.Search_metadata
import tachiyomi.data.Search_tags
import tachiyomi.data.Search_titles

/** Domain models from the generated search-metadata rows (SY). */
public object MangaMetadataMapper {
    /** The [SearchMetadata] of a `search_metadata` row. */
    public fun mapMetadata(row: Search_metadata): SearchMetadata = SearchMetadata(
        mangaId = row.manga_id,
        uploader = row.uploader,
        extra = row.extra,
        indexedExtra = row.indexed_extra,
        extraVersion = row.extra_version.toInt(),
    )

    /** The [SearchTitle] of a `search_titles` row. */
    public fun mapTitle(row: Search_titles): SearchTitle = SearchTitle(
        mangaId = row.manga_id,
        id = row._id,
        title = row.title,
        type = row.type.toInt(),
    )

    /** The [SearchTag] of a `search_tags` row. */
    public fun mapTag(row: Search_tags): SearchTag = SearchTag(
        mangaId = row.manga_id,
        id = row._id,
        namespace = row.namespace,
        name = row.name,
        type = row.type.toInt(),
    )
}
