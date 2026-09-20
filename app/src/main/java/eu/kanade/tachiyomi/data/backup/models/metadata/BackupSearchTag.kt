package eu.kanade.tachiyomi.data.backup.models.metadata

import exh.metadata.sql.models.SearchTag
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

private const val BACKUP_SEARCH_TAG_NAMESPACE = 1
private const val BACKUP_SEARCH_TAG_NAME = 2
private const val BACKUP_SEARCH_TAG_TYPE = 3

@Serializable
internal data class BackupSearchTag(
    @ProtoNumber(BACKUP_SEARCH_TAG_NAMESPACE) val namespace: String? = null,
    @ProtoNumber(BACKUP_SEARCH_TAG_NAME) val name: String,
    @ProtoNumber(BACKUP_SEARCH_TAG_TYPE) val type: Int,
) {

    companion object {
        fun copyFrom(searchTag: SearchTag): BackupSearchTag {
            return BackupSearchTag(
                namespace = searchTag.namespace,
                name = searchTag.name,
                type = searchTag.type,
            )
        }
    }
}

internal fun BackupSearchTag.getSearchTag(mangaId: Long): SearchTag {
    return SearchTag(
        id = null,
        mangaId = mangaId,
        namespace = namespace,
        name = name,
        type = type,
    )
}
