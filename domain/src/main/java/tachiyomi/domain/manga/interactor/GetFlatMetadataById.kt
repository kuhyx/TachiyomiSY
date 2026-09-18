package tachiyomi.domain.manga.interactor

import eu.kanade.tachiyomi.source.online.MetadataSource
import exh.metadata.metadata.base.FlatMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.repository.MangaMetadataRepository

/** Assembles a manga's search metadata, tags and titles into one [FlatMetadata]. */
public class GetFlatMetadataById(
    private val mangaMetadataRepository: MangaMetadataRepository,
) : MetadataSource.GetFlatMetadataById {

    override suspend fun await(id: Long): FlatMetadata? {
        return try {
            mangaMetadataRepository.getMetadataById(id)?.let { meta ->
                val tags = mangaMetadataRepository.getTagsById(id)
                val titles = mangaMetadataRepository.getTitlesById(id)
                FlatMetadata(meta, tags, titles)
            }
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            null
        }
    }

    /** The flat metadata of manga [id] as a flow; emits null while it has no metadata row. */
    public fun subscribe(id: Long): Flow<FlatMetadata?> {
        return combine(
            mangaMetadataRepository.subscribeMetadataById(id),
            mangaMetadataRepository.subscribeTagsById(id),
            mangaMetadataRepository.subscribeTitlesById(id),
        ) { meta, tags, titles ->
            meta?.let { FlatMetadata(it, tags, titles) }
        }
    }
}
