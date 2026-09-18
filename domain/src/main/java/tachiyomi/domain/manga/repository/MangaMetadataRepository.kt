package tachiyomi.domain.manga.repository

import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata

/**
 * Persistence of source search metadata (SY). Reads live in
 * [MangaMetadataReadRepository]; the writes are here.
 */
public interface MangaMetadataRepository : MangaMetadataReadRepository {

    /** Stores [flatMetadata], replacing the manga's previous metadata, tags and titles. */
    public suspend fun insertFlatMetadata(flatMetadata: FlatMetadata)

    /** Flattens [metadata] and stores it. */
    public suspend fun insertMetadata(metadata: RaisedSearchMetadata) {
        insertFlatMetadata(metadata.flatten())
    }
}
