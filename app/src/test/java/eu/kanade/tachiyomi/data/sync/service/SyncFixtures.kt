package eu.kanade.tachiyomi.data.sync.service

import android.content.Context
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import io.mockk.mockk
import kotlinx.serialization.json.Json

/** A [SyncService] that answers [doSync] with [remote] and opens the protected helpers to the tests. */
internal class FakeSyncService(
    preferences: SyncPreferences = SyncPreferences(FlowPreferenceStore()),
    context: Context = mockk(),
) : SyncService(context, Json, preferences) {
    var remote: Backup? = null

    override suspend fun doSync(syncData: SyncData): Backup? = remote

    fun guard(entryCount: Int) = assertNoLibraryCollapse(entryCount)

    fun merge(local: SyncData, remote: SyncData): SyncData = mergeSyncData(local, remote)
}

internal fun manga(
    url: String,
    version: Long = 0,
    lastModifiedAt: Long = 0,
    favorite: Boolean = true,
    categories: List<Long> = emptyList(),
    chapters: List<BackupChapter> = emptyList(),
): BackupManga = BackupManga(
    source = 1L,
    url = url,
    title = url,
    chapters = chapters,
    categories = categories,
    favorite = favorite,
    lastModifiedAt = lastModifiedAt,
    version = version,
)

internal fun chapter(
    url: String,
    version: Long = 0,
    lastModifiedAt: Long = 0,
    sourceOrder: Long = 0,
): BackupChapter = BackupChapter(
    url = url,
    name = url,
    sourceOrder = sourceOrder,
    lastModifiedAt = lastModifiedAt,
    version = version,
)

internal fun category(
    name: String,
    order: Long = 0,
    version: Long = 0,
    uid: Long = 0,
    lastModifiedAt: Long = 0,
): BackupCategory = BackupCategory(
    name = name,
    order = order,
    version = version,
    uid = uid,
    lastModifiedAt = lastModifiedAt,
)

internal fun backup(vararg manga: BackupManga): Backup = Backup(backupManga = manga.toList())
