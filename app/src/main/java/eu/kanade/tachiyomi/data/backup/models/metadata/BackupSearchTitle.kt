package eu.kanade.tachiyomi.data.backup.models.metadata

import exh.metadata.sql.models.SearchTitle
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

private const val BACKUP_SEARCH_TITLE_TITLE = 1
private const val BACKUP_SEARCH_TITLE_TYPE = 2

@Serializable
internal data class BackupSearchTitle(
    @ProtoNumber(BACKUP_SEARCH_TITLE_TITLE) val title: String,
    @ProtoNumber(BACKUP_SEARCH_TITLE_TYPE) val type: Int,
) {

    companion object {
        fun copyFrom(searchTitle: SearchTitle): BackupSearchTitle {
            return BackupSearchTitle(
                title = searchTitle.title,
                type = searchTitle.type,
            )
        }
    }
}

internal fun BackupSearchTitle.getSearchTitle(mangaId: Long): SearchTitle {
    return SearchTitle(
        id = null,
        mangaId = mangaId,
        title = title,
        type = type,
    )
}
