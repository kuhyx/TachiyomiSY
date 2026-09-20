package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

private const val BACKUP_SOURCE_NAME = 1
private const val BACKUP_SOURCE_SOURCE_ID = 2

@Serializable
internal data class BackupSource(
    @ProtoNumber(BACKUP_SOURCE_NAME) val name: String = "",
    @ProtoNumber(BACKUP_SOURCE_SOURCE_ID) val sourceId: Long,
)
