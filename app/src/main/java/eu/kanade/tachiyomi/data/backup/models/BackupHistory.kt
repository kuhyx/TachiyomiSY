package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.history.model.History
import java.util.Date

private const val BACKUP_HISTORY_URL = 1
private const val BACKUP_HISTORY_LAST_READ = 2
private const val BACKUP_HISTORY_READ_DURATION = 3

@Serializable
internal data class BackupHistory(
    @ProtoNumber(BACKUP_HISTORY_URL) var url: String,
    @ProtoNumber(BACKUP_HISTORY_LAST_READ) var lastRead: Long,
    @ProtoNumber(BACKUP_HISTORY_READ_DURATION) var readDuration: Long = 0,
) {
    fun getHistoryImpl(): History {
        return History.create().copy(
            readAt = Date(lastRead),
            readDuration = readDuration,
        )
    }
}
