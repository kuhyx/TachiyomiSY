package eu.kanade.tachiyomi.data.sync.service

import android.content.Context
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class SyncData(
    val deviceId: String = "",
    val backup: Backup? = null,
)

/**
 * Raised when a merge would push a collapsed library over a healthy remote one.
 *
 * Sync is fail-closed on this: aborting costs one skipped sync, whereas pushing
 * overwrites the last good copy on the server and there is nothing left to
 * recover from.
 */
internal class SyncCollapseException(message: String) : Exception(message)

internal abstract class SyncService(
    val context: Context,
    val json: Json,
    val syncPreferences: SyncPreferences,
) {
    abstract suspend fun doSync(syncData: SyncData): Backup?

    /**
     * Refuse to push a payload that lost entries since this device's last push.
     *
     * @param entryCount Library entries in the payload about to be uploaded.
     * @throws SyncCollapseException If the payload shrank by more than
     *   [MAX_REMOTE_DROP_RATIO] of the last pushed one.
     */
    protected fun assertNoLibraryCollapse(entryCount: Int) {
        val baseline = syncPreferences.lastSyncEntryCount.get()
        if (baseline <= 0) return
        if (entryCount >= baseline * (1 - MAX_REMOTE_DROP_RATIO)) return
        throw SyncCollapseException(
            "Refusing to sync: this device now has $entryCount library entries, down from " +
                "$baseline at the last sync. Restore this device from a backup before syncing again.",
        )
    }

    /**
     * Merges the local and remote sync data into a single JSON string.
     *
     * @param localSyncData The SData containing the local sync data.
     * @param remoteSyncData The SData containing the remote sync data.
     * @return The JSON string containing the merged sync data.
     */
    protected fun mergeSyncData(localSyncData: SyncData, remoteSyncData: SyncData): SyncData {
        val mergedCategoriesList =
            mergeCategoriesLists(localSyncData.backup?.backupCategories, remoteSyncData.backup?.backupCategories)

        val mergedMangaList = mergeMangaLists(
            localSyncData.backup?.backupManga,
            remoteSyncData.backup?.backupManga,
            localSyncData.backup?.backupCategories ?: emptyList(),
            remoteSyncData.backup?.backupCategories ?: emptyList(),
            mergedCategoriesList,
        )

        val mergedSourcesList =
            mergeSourcesLists(localSyncData.backup?.backupSources, remoteSyncData.backup?.backupSources)
        val mergedPreferencesList =
            mergePreferencesLists(localSyncData.backup?.backupPreferences, remoteSyncData.backup?.backupPreferences)
        val mergedSourcePreferencesList = mergeSourcePreferencesLists(
            localSyncData.backup?.backupSourcePreferences,
            remoteSyncData.backup?.backupSourcePreferences,
        )

        // SY -->
        val mergedSavedSearchesList = mergeSavedSearchesLists(
            localSyncData.backup?.backupSavedSearches,
            remoteSyncData.backup?.backupSavedSearches,
        )
        // SY <--

        // Create the merged Backup object
        val mergedBackup = Backup(
            backupManga = mergedMangaList,
            backupCategories = mergedCategoriesList,
            backupSources = mergedSourcesList,
            backupPreferences = mergedPreferencesList,
            backupSourcePreferences = mergedSourcePreferencesList,

            // SY -->
            backupSavedSearches = mergedSavedSearchesList,
            // SY <--
        )

        // Create the merged SData object
        return SyncData(
            deviceId = syncPreferences.uniqueDeviceID(),
            backup = mergedBackup,
        )
    }

// Merges two lists of BackupChapter objects, selecting the most recent chapter based on the lastModifiedAt value.
// If lastModifiedAt is null for a chapter, it treats that chapter as the oldest possible for comparison purposes.
// This function is designed to reconcile local and remote chapter lists, ensuring the most up-to-date chapter is
// retained.
// @param localChapters The list of local BackupChapter objects.
// @param remoteChapters The list of remote BackupChapter objects.
// @return A list of BackupChapter objects, each representing the most recent version of the chapter from either local
// or remote sources.
// - This function is used in scenarios where local and remote chapter lists need to be synchronized.
// - It iterates over the union of the URLs from both local and remote chapters.
// - For each URL, it compares the corresponding local and remote chapters based on the lastModifiedAt value.
// - If only one source (local or remote) has the chapter for a URL, that chapter is used.
// - If both sources have the chapter, the one with the more recent lastModifiedAt value is chosen.
// - If lastModifiedAt is null or missing, the chapter is considered the oldest for safety, ensuring that any chapter
// with a valid timestamp is preferred.
// - The resulting list contains the most recent chapters from the combined set of local and remote chapters.

    // SY <--

    companion object {
        // Share of the server's entries that may vanish from this device before the
        // merge is treated as local data loss. Removing a handful of finished series
        // is normal; a tenth of the library disappearing at once is not.
        internal const val MAX_REMOTE_DROP_RATIO = 0.10
    }
}
