package tachiyomi.domain.manga.interactor

import eu.kanade.tachiyomi.source.online.MetadataSource
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.repository.MangaMetadataRepository

/** Saves a manga's search metadata, tags and titles, in flat or raised form. */
public class InsertFlatMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) : MetadataSource.InsertFlatMetadata {

    /** Writes [flatMetadata] and its tags and titles; logs and returns normally when the store fails. */
    public suspend fun await(flatMetadata: FlatMetadata) {
        try {
            mangaMetadataRepository.insertFlatMetadata(flatMetadata)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
        }
    }

    override suspend fun await(metadata: RaisedSearchMetadata) {
        try {
            mangaMetadataRepository.insertMetadata(metadata)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
        }
    }
}
