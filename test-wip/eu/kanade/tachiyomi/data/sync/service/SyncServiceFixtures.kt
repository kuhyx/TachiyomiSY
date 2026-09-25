package eu.kanade.tachiyomi.data.sync.service

import android.content.Context
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.mockk.mockk
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.LogcatLogger

/**
 * A [SyncService] whose [doSync] just returns [result], so the protected merge and guard helpers
 * can be exercised without a transport.
 */
internal class FakeSyncService(
    context: Context = mockk(relaxed = true),
    json: Json = Json,
    preferences: SyncPreferences,
) : SyncService(context, json, preferences) {

    var result: Backup? = null

    override suspend fun doSync(syncData: SyncData): Backup? = result

    /** [SyncService.assertNoLibraryCollapse] is protected; this opens it to the tests. */
    fun assertNoCollapse(entryCount: Int): Unit = assertNoLibraryCollapse(entryCount)

    /** [SyncService.mergeSyncData] is protected; this opens it to the tests. */
    fun merge(local: SyncData, remote: SyncData): SyncData = mergeSyncData(local, remote)
}

/** Collects every message the code under test logs, and keeps [logcat.logcat] lambdas running. */
internal class RecordingLogger : LogcatLogger {
    val messages: MutableList<String> = mutableListOf()

    override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

    override fun log(priority: LogPriority, tag: String, message: String) {
        messages += message
    }
}

/**
 * Installs a [RecordingLogger]; without an installed logger `logcat { ... }` never evaluates its
 * lambda, so every message body would stay uncovered.
 */
internal fun installRecordingLogger(): RecordingLogger {
    val logger = RecordingLogger()
    if (!LogcatLogger.isInstalled) {
        LogcatLogger.install()
    }
    LogcatLogger.loggers += logger
    return logger
}

/** Undoes [installRecordingLogger]. */
internal fun removeRecordingLogger(logger: RecordingLogger) {
    LogcatLogger.loggers -= logger
    if (LogcatLogger.loggers.isEmpty()) {
        LogcatLogger.uninstall()
    }
}

/** A sync service backed by a real in-memory preference store the test can write to. */
internal fun fakeSyncService(store: MapPreferenceStore = MapPreferenceStore()): FakeSyncService =
    FakeSyncService(preferences = SyncPreferences(store))

internal fun syncCategory(
    name: String,
    order: Long = 0,
    uid: Long = 0,
    version: Long = 0,
    lastModifiedAt: Long = 0,
): BackupCategory = BackupCategory(
    name = name,
    order = order,
    uid = uid,
    version = version,
    lastModifiedAt = lastModifiedAt,
)

internal fun syncManga(
    url: String,
    source: Long = 1L,
    version: Long = 0,
    lastModifiedAt: Long = 0,
    favorite: Boolean = true,
    categories: List<Long> = emptyList(),
    chapters: List<BackupChapter> = emptyList(),
): BackupManga = BackupManga(
    source = source,
    url = url,
    title = url,
    version = version,
    lastModifiedAt = lastModifiedAt,
    favorite = favorite,
    categories = categories,
    chapters = chapters,
)

internal fun syncChapter(
    url: String,
    version: Long = 0,
    lastModifiedAt: Long = 0,
    sourceOrder: Long = 0,
): BackupChapter = BackupChapter(
    url = url,
    name = url,
    version = version,
    lastModifiedAt = lastModifiedAt,
    sourceOrder = sourceOrder,
)
