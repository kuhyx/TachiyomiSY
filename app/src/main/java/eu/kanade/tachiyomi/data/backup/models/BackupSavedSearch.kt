package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/*
* SY saved searches class
 */
private const val BACKUP_SAVED_SEARCH_NAME = 1
private const val BACKUP_SAVED_SEARCH_QUERY = 2
private const val BACKUP_SAVED_SEARCH_FILTER_LIST = 3
private const val BACKUP_SAVED_SEARCH_SOURCE = 4

@Serializable
internal data class BackupSavedSearch(
    @ProtoNumber(BACKUP_SAVED_SEARCH_NAME) val name: String,
    @ProtoNumber(BACKUP_SAVED_SEARCH_QUERY) val query: String = "",
    @ProtoNumber(BACKUP_SAVED_SEARCH_FILTER_LIST) val filterList: String = "",
    @ProtoNumber(BACKUP_SAVED_SEARCH_SOURCE) val source: Long = 0,
)

internal val backupSavedSearchMapper =
    { _: Long, source: Long, name: String, query: String?, filtersJson: String? ->
        BackupSavedSearch(
            source = source,
            name = name,
            query = query.orEmpty(),
            filterList = filtersJson ?: "[]",
        )
    }
