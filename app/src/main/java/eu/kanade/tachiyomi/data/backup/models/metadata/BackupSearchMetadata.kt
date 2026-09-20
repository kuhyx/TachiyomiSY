package eu.kanade.tachiyomi.data.backup.models.metadata

import exh.metadata.sql.models.SearchMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

private const val BACKUP_SEARCH_METADATA_UPLOADER = 1
private const val BACKUP_SEARCH_METADATA_EXTRA = 2
private const val BACKUP_SEARCH_METADATA_INDEXED_EXTRA = 3
private const val BACKUP_SEARCH_METADATA_EXTRA_VERSION = 4

@Serializable
internal data class BackupSearchMetadata(
    @ProtoNumber(BACKUP_SEARCH_METADATA_UPLOADER) val uploader: String? = null,
    @ProtoNumber(BACKUP_SEARCH_METADATA_EXTRA) val extra: String,
    @ProtoNumber(BACKUP_SEARCH_METADATA_INDEXED_EXTRA) val indexedExtra: String? = null,
    @ProtoNumber(BACKUP_SEARCH_METADATA_EXTRA_VERSION) val extraVersion: Int,
) {

    companion object {
        fun copyFrom(searchMetadata: SearchMetadata): BackupSearchMetadata {
            return BackupSearchMetadata(
                uploader = searchMetadata.uploader,
                extra = searchMetadata.extra,
                indexedExtra = searchMetadata.indexedExtra,
                extraVersion = searchMetadata.extraVersion,
            )
        }
    }
}

internal fun BackupSearchMetadata.getSearchMetadata(mangaId: Long): SearchMetadata {
    return SearchMetadata(
        mangaId = mangaId,
        uploader = uploader,
        extra = extra,
        indexedExtra = indexedExtra,
        extraVersion = extraVersion,
    )
}
