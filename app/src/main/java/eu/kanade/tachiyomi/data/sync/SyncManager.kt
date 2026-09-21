package eu.kanade.tachiyomi.data.sync

import android.content.Context
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.sync.models.SyncSettings
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.data.backup.create.backupCategories
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.backup.restore.restorers.MangaRestorer
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveSyncService
import eu.kanade.tachiyomi.data.sync.service.SyncData
import eu.kanade.tachiyomi.data.sync.service.SyncYomiSyncService
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.logcat
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.domain.category.interactor.GetCategories
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import eu.kanade.tachiyomi.data.sync.service.SyncService as RemoteSyncService

// A manager to handle synchronization tasks in the app, such as updating
// sync preferences and performing synchronization with a remote server.
// @property context The application context.

internal class SyncManager(
    private val context: Context,
    internal val database: Database = Injekt.get(),
    private val syncPreferences: SyncPreferences = Injekt.get(),
    private var json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    },
    internal val getCategories: GetCategories = Injekt.get(),
) {
    private val backupCreator: BackupCreator = BackupCreator(context, false)
    private val notifier: SyncNotifier = SyncNotifier(context)
    internal val mangaRestorer: MangaRestorer = MangaRestorer()

    enum class SyncService(val value: Int) {
        NONE(0),
        SYNCYOMI(1),
        GOOGLE_DRIVE(2),
        ;

        companion object {
            fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: NONE
        }
    }

    /**
     * Syncs data with a sync service.
     *
     * This function retrieves local data (favorites, manga, extensions, and categories)
     * from the database using the BackupManager, then synchronizes the data with a sync service.
     */
    suspend fun syncData() {
        // Reset isSyncing in case it was left over or failed syncing during restore.
        database.transaction {
            database.mangasQueries.resetIsSyncing()
            database.chaptersQueries.resetIsSyncing()
            database.categoriesQueries.resetIsSyncing()
        }

        val syncOptions = syncPreferences.getSyncSettings()
        val databaseManga = getAllMangaThatNeedsSync()
        val backup = backupCreator.createSyncBackup(databaseManga, syncOptions.toBackupOptions())
        val syncData = SyncData(deviceId = syncPreferences.uniqueDeviceID(), backup = backup)
        val remoteBackup = createSyncService()?.doSync(syncData)

        when {
            // should we call showSyncError?
            remoteBackup == null -> {
                logcat(LogPriority.DEBUG) { "Skip restore due to network issues" }
            }
            remoteBackup === backup -> {
                // nothing changed
                logcat(LogPriority.DEBUG) { "Skip restore due to remote was overwrite from local" }
                markSynced("Sync completed successfully")
            }
            remoteBackup.isEmptyRemote() -> {
                notifier.showSyncError("No data found on remote server.")
            }
            // First sync with a populated library: the remote was just seeded, nothing to restore.
            syncPreferences.lastSyncTimestamp.get() == 0L && databaseManga.isNotEmpty() -> {
                markSynced("Updated remote data successfully")
            }
            else -> {
                restoreRemote(backup, remoteBackup, syncOptions)
            }
        }
    }

    private fun createSyncService(): RemoteSyncService? {
        return when (val syncService = SyncService.fromInt(syncPreferences.syncService.get())) {
            SyncService.SYNCYOMI -> {
                SyncYomiSyncService(context, json, syncPreferences, notifier)
            }
            SyncService.GOOGLE_DRIVE -> {
                GoogleDriveSyncService(context, json, syncPreferences)
            }
            else -> {
                logcat(LogPriority.ERROR) { "Invalid sync service type: $syncService" }
                null
            }
        }
    }

    private fun markSynced(message: String) {
        syncPreferences.lastSyncTimestamp.set(Date().time)
        notifier.showSyncSuccess(message)
    }

    // Merges the remote backup over the local one and hands the result to [BackupRestoreJob].
    private suspend fun restoreRemote(backup: Backup, remoteBackup: Backup, syncOptions: SyncSettings) {
        val (filteredFavorites, nonFavorites) = filterFavoritesAndNonFavorites(remoteBackup)
        updateNonFavorites(nonFavorites)
        if (!backup.hasChangesFrom(remoteBackup, filteredFavorites)) {
            markSynced("Sync completed successfully")
            return
        }

        val newSyncData = backup.mergedWithRemote(remoteBackup, filteredFavorites)
        if (syncOptions.categories) {
            deleteCategoriesMissingFrom(newSyncData)
        }

        val backupUri = writeSyncDataToCache(context, newSyncData)
        logcat(LogPriority.DEBUG) { "Got Backup Uri: $backupUri" }
        if (backupUri == null) {
            logcat(LogPriority.ERROR) { "Failed to write sync data to file" }
            return
        }
        BackupRestoreJob.start(context, backupUri, sync = true, options = syncOptions.toRestoreOptions())
        // update the sync timestamp
        syncPreferences.lastSyncTimestamp.set(Date().time)
    }

    // Drops local categories the merged backup no longer knows by uid or by name.
    private suspend fun deleteCategoriesMissingFrom(merged: Backup) {
        val mergedUids = merged.backupCategories.map { it.uid }.toSet()
        val mergedNames = merged.backupCategories.map { it.name }.toSet()
        val localCategories = getCategories.await().filterNot { it.id == 0L } // Exclude system category
        val categoriesToDelete = localCategories.filter { it.uid !in mergedUids && it.name !in mergedNames }
        if (categoriesToDelete.isNotEmpty()) {
            database.transaction {
                categoriesToDelete.forEach { database.categoriesQueries.delete(it.id) }
            }
        }
    }
}
