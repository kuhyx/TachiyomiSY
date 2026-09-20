package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.backup.models.metadata.BackupSearchMetadata
import eu.kanade.tachiyomi.data.backup.models.metadata.BackupSearchTag
import eu.kanade.tachiyomi.data.backup.models.metadata.BackupSearchTitle
import eu.kanade.tachiyomi.data.backup.models.metadata.getSearchMetadata
import eu.kanade.tachiyomi.data.backup.models.metadata.getSearchTag
import eu.kanade.tachiyomi.data.backup.models.metadata.getSearchTitle
import exh.metadata.metadata.base.FlatMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

private const val BACKUP_FLAT_METADATA_SEARCH_METADATA = 1
private const val BACKUP_FLAT_METADATA_SEARCH_TAGS = 2
private const val BACKUP_FLAT_METADATA_SEARCH_TITLES = 3

@Serializable
internal data class BackupFlatMetadata(
    @ProtoNumber(BACKUP_FLAT_METADATA_SEARCH_METADATA) var searchMetadata: BackupSearchMetadata,
    @ProtoNumber(BACKUP_FLAT_METADATA_SEARCH_TAGS) var searchTags: List<BackupSearchTag> = emptyList(),
    @ProtoNumber(BACKUP_FLAT_METADATA_SEARCH_TITLES) var searchTitles: List<BackupSearchTitle> = emptyList(),
) {

    companion object {
        fun copyFrom(flatMetadata: FlatMetadata): BackupFlatMetadata {
            return BackupFlatMetadata(
                searchMetadata = BackupSearchMetadata.copyFrom(flatMetadata.metadata),
                searchTags = flatMetadata.tags.map { BackupSearchTag.copyFrom(it) },
                searchTitles = flatMetadata.titles.map { BackupSearchTitle.copyFrom(it) },
            )
        }
    }
}

internal fun BackupFlatMetadata.getFlatMetadata(mangaId: Long): FlatMetadata {
    return FlatMetadata(
        metadata = searchMetadata.getSearchMetadata(mangaId),
        tags = searchTags.map { it.getSearchTag(mangaId) },
        titles = searchTitles.map { it.getSearchTitle(mangaId) },
    )
}
