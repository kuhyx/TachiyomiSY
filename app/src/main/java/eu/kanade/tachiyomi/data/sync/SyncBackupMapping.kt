package eu.kanade.tachiyomi.data.sync

import android.content.Context
import android.net.Uri
import eu.kanade.domain.sync.models.SyncSettings
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.create.backupAppPreferences
import eu.kanade.tachiyomi.data.backup.create.backupCategories
import eu.kanade.tachiyomi.data.backup.create.backupExtensionStores
import eu.kanade.tachiyomi.data.backup.create.backupMangas
import eu.kanade.tachiyomi.data.backup.create.backupSavedSearches
import eu.kanade.tachiyomi.data.backup.create.backupSourcePreferences
import eu.kanade.tachiyomi.data.backup.create.backupSources
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Chapters
import tachiyomi.domain.manga.model.Manga
import java.io.File
import java.io.IOException

// Pure mappings between the sync settings, the backup model and the restore options; the
// stateful orchestration stays in [SyncManager].

internal fun SyncSettings.toBackupOptions(): BackupOptions = BackupOptions(
    libraryEntries = libraryEntries,
    categories = categories,
    chapters = chapters,
    tracking = tracking,
    history = history,
    extensionStores = extensionStores,
    appSettings = appSettings,
    sourceSettings = sourceSettings,
    privateSettings = privateSettings,
    // SY -->
    customInfo = customInfo,
    readEntries = readEntries,
    savedSearches = savedSearches,
    // SY <--
)

internal fun SyncSettings.toRestoreOptions(): RestoreOptions = RestoreOptions(
    appSettings = appSettings,
    sourceSettings = sourceSettings,
    libraryEntries = libraryEntries,
    categories = categories,
    extensionStores = extensionStores,
    // SY -->
    savedSearches = savedSearches,
    // SY <--
)

internal suspend fun BackupCreator.createSyncBackup(manga: List<Manga>, options: BackupOptions): Backup {
    logcat(LogPriority.DEBUG) { "Begin create backup" }
    val backupManga = backupMangas(manga, options)
    val backup = Backup(
        backupManga = backupManga,
        backupCategories = backupCategories(options),
        backupSources = backupSources(backupManga),
        backupPreferences = backupAppPreferences(options),
        backupSourcePreferences = backupSourcePreferences(options),
        backupExtensionStores = backupExtensionStores(options),
        // SY -->
        backupSavedSearches = backupSavedSearches(options),
        // SY <--
    )
    logcat(LogPriority.DEBUG) { "End create backup" }
    return backup
}

/** A remote with no library, categories or sources is a fresh account, not something to restore. */
internal fun Backup.isEmptyRemote(): Boolean =
    backupManga.isEmpty() && backupCategories.isEmpty() && backupSources.isEmpty()

/** The local backup with every remote-owned section replaced, and the library reduced to [favorites]. */
internal fun Backup.mergedWithRemote(remote: Backup, favorites: List<BackupManga>): Backup = copy(
    backupManga = favorites,
    backupCategories = remote.backupCategories,
    backupSources = remote.backupSources,
    backupPreferences = remote.backupPreferences,
    backupSourcePreferences = remote.backupSourcePreferences,
    backupExtensionStores = remote.backupExtensionStores,
    // SY -->
    backupSavedSearches = remote.backupSavedSearches,
    // SY <--
)

/**
 * Whether restoring [remote] over this backup would change anything ([favorites] already filtered to the
 * changed ones).
 */
internal fun Backup.hasChangesFrom(remote: Backup, favorites: List<BackupManga>): Boolean = listOf(
    favorites.isNotEmpty(),
    remote.backupCategories != backupCategories,
    remote.backupSources != backupSources,
    remote.backupPreferences != backupPreferences,
    remote.backupSourcePreferences != backupSourcePreferences,
    remote.backupExtensionStores != backupExtensionStores,
    remote.backupSavedSearches != backupSavedSearches,
).any { it }

internal fun areChaptersDifferent(localChapters: List<Chapters>, remoteChapters: List<BackupChapter>): Boolean {
    val localChapterMap = localChapters.associateBy { it.url }
    val remoteChapterMap = remoteChapters.associateBy { it.url }
    if (localChapterMap.size != remoteChapterMap.size) {
        return true
    }
    // A chapter missing on the remote, or on a different version, makes the lists differ.
    return localChapterMap.any { (url, localChapter) ->
        val remoteChapter = remoteChapterMap[url]
        remoteChapter == null || localChapter.version != remoteChapter.version
    }
}

internal fun writeSyncDataToCache(context: Context, backup: Backup): Uri? {
    val cacheFile = File(context.cacheDir, "tachiyomi_sync_data.proto.gz")
    return try {
        cacheFile.outputStream().use { output ->
            output.write(ProtoBuf.encodeToByteArray(Backup.serializer(), backup))
            Uri.fromFile(cacheFile)
        }
    } catch (e: IOException) {
        logcat("SyncManager", LogPriority.ERROR) { "Failed to write sync data to cache\n${e.asLog()}" }
        null
    }
}
