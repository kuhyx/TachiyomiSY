package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import logcat.LogPriority
import logcat.logcat
import kotlin.time.Duration.Companion.seconds

private const val CATEGORY_LOG_TAG = "MergeCategories"

// Merges two lists of SyncCategory objects, prioritizing the category with the most recent order value.
// @param localCategoriesList The list of local SyncCategory objects.
// @param remoteCategoriesList The list of remote SyncCategory objects.
// @return The merged list of SyncCategory objects.
internal fun SyncService.mergeCategoriesLists(
    localCategoriesList: List<BackupCategory>?,
    remoteCategoriesList: List<BackupCategory>?,
): List<BackupCategory> {
    if (localCategoriesList == null) return remoteCategoriesList ?: emptyList()
    if (remoteCategoriesList == null) return localCategoriesList

    val result = mutableListOf<BackupCategory>()
    val processedLocals = mutableSetOf<BackupCategory>()
    val localMapByUid = localCategoriesList.filter { it.uid != 0L }.associateBy { it.uid }
    val localMapByName = localCategoriesList.associateBy { it.name }
    val lastSyncTime = syncPreferences.lastSyncTimestamp.get()

    remoteCategoriesList.forEach { remote ->
        // Match by UID first, by name as the fallback.
        val localMatch = remote.uid.takeIf { it != 0L }?.let { localMapByUid[it] } ?: localMapByName[remote.name]
        if (localMatch != null) {
            processedLocals.add(localMatch)
            result.add(newerCategory(localMatch, remote))
        } else if (isCategoryModifiedSince(remote, lastSyncTime)) {
            logcat(CATEGORY_LOG_TAG, LogPriority.DEBUG) {
                "Adding new remote category: ${remote.name} (UID: ${remote.uid})"
            }
            result.add(remote)
        } else {
            logcat(CATEGORY_LOG_TAG, LogPriority.DEBUG) {
                "Dropping deleted remote category: ${remote.name} (UID: ${remote.uid})"
            }
        }
    }

    // Add remaining Local Categories
    localCategoriesList.filterNot { it in processedLocals }.forEach { local ->
        if (isCategoryModifiedSince(local, lastSyncTime)) {
            logcat(CATEGORY_LOG_TAG, LogPriority.DEBUG) {
                "Keeping local only category: ${local.name} (UID: ${local.uid})"
            }
            result.add(local)
        } else {
            logcat(CATEGORY_LOG_TAG, LogPriority.DEBUG) {
                "Dropping local category deleted on remote: ${local.name} (UID: ${local.uid})"
            }
        }
    }
    return result.sortedBy { it.order }
}

private fun isCategoryModifiedSince(category: BackupCategory, lastSyncTime: Long): Boolean =
    lastSyncTime == 0L || category.lastModifiedAt.seconds.inWholeMilliseconds > lastSyncTime

// Conflict resolution: the higher version wins; a remote without a uid inherits the local one.
private fun newerCategory(localMatch: BackupCategory, remote: BackupCategory): BackupCategory {
    return if (localMatch.version >= remote.version) {
        logcat(CATEGORY_LOG_TAG, LogPriority.DEBUG) {
            "Keeping local category: ${localMatch.name} (UID: ${localMatch.uid})"
        }
        localMatch
    } else {
        logcat(CATEGORY_LOG_TAG, LogPriority.DEBUG) { "Keeping remote category: ${remote.name} (UID: ${remote.uid})" }
        if (remote.uid == 0L) {
            remote.uid = localMatch.uid
        }
        remote
    }
}

internal fun SyncService.mergeSourcesLists(
    localSources: List<BackupSource>?,
    remoteSources: List<BackupSource>?,
): List<BackupSource> {
    val logTag = "MergeSources"

    // Create maps using sourceId as key
    val localSourceMap = localSources?.associateBy { it.sourceId } ?: emptyMap()
    val remoteSourceMap = remoteSources?.associateBy { it.sourceId } ?: emptyMap()

    logcat(LogPriority.DEBUG, logTag) {
        "Starting source merge. Local sources: ${localSources?.size}, Remote sources: ${remoteSources?.size}"
    }

    // Merge both source maps
    val mergedSources = (localSourceMap.keys + remoteSourceMap.keys).distinct().mapNotNull { sourceId ->
        val localSource = localSourceMap[sourceId]
        val remoteSource = remoteSourceMap[sourceId]

        logcat(LogPriority.DEBUG, logTag) {
            "Processing source ID: $sourceId. Local source: ${localSource != null}, " +
                "Remote source: ${remoteSource != null}"
        }

        when {
            localSource != null && remoteSource == null -> {
                logcat(LogPriority.DEBUG, logTag) { "Using local source: ${localSource.name}." }
                localSource
            }
            remoteSource != null && localSource == null -> {
                logcat(LogPriority.DEBUG, logTag) { "Using remote source: ${remoteSource.name}." }
                remoteSource
            }
            else -> {
                logcat(
                    LogPriority.DEBUG,
                    logTag,
                ) { "Remote and local have the same source ID: $sourceId. Keeping local." }
                localSource
            }
        }
    }

    logcat(LogPriority.DEBUG, logTag) { "Source merge completed. Total merged sources: ${mergedSources.size}" }

    return mergedSources
}

internal fun SyncService.mergePreferencesLists(
    localPreferences: List<BackupPreference>?,
    remotePreferences: List<BackupPreference>?,
): List<BackupPreference> {
    val logTag = "MergePreferences"

    // Create maps using key as the unique identifier
    val localPreferencesMap = localPreferences?.associateBy { it.key } ?: emptyMap()
    val remotePreferencesMap = remotePreferences?.associateBy { it.key } ?: emptyMap()

    logcat(LogPriority.DEBUG, logTag) {
        "Starting preferences merge. Local preferences: ${localPreferences?.size}, " +
            "Remote preferences: ${remotePreferences?.size}"
    }

    // Merge both preferences maps
    val mergedPreferences = (localPreferencesMap.keys + remotePreferencesMap.keys).distinct().mapNotNull { key ->
        val localPreference = localPreferencesMap[key]
        val remotePreference = remotePreferencesMap[key]

        logcat(LogPriority.DEBUG, logTag) {
            "Processing preference key: $key. Local preference: ${localPreference != null}, " +
                "Remote preference: ${remotePreference != null}"
        }

        when {
            localPreference != null && remotePreference == null -> {
                logcat(LogPriority.DEBUG, logTag) { "Using local preference: ${localPreference.key}." }
                localPreference
            }
            remotePreference != null && localPreference == null -> {
                logcat(LogPriority.DEBUG, logTag) { "Using remote preference: ${remotePreference.key}." }
                remotePreference
            }
            else -> {
                logcat(
                    LogPriority.DEBUG,
                    logTag,
                ) { "Both remote and local have the same preference key: $key. Keeping local." }
                localPreference
            }
        }
    }

    logcat(LogPriority.DEBUG, logTag) {
        "Preferences merge completed. Total merged preferences: ${mergedPreferences.size}"
    }

    return mergedPreferences
}

internal fun SyncService.mergeSourcePreferencesLists(
    localPreferences: List<BackupSourcePreferences>?,
    remotePreferences: List<BackupSourcePreferences>?,
): List<BackupSourcePreferences> {
    val logTag = "MergeSourcePreferences"

    // Create maps using sourceKey as the unique identifier
    val localPreferencesMap = localPreferences?.associateBy { it.sourceKey } ?: emptyMap()
    val remotePreferencesMap = remotePreferences?.associateBy { it.sourceKey } ?: emptyMap()

    logcat(LogPriority.DEBUG, logTag) {
        "Starting source preferences merge. Local source preferences: ${localPreferences?.size}, " +
            "Remote source preferences: ${remotePreferences?.size}"
    }

    // Merge both source preferences maps
    val mergedSourcePreferences = (localPreferencesMap.keys + remotePreferencesMap.keys).distinct()
        .mapNotNull { sourceKey ->
            val localSourcePreference = localPreferencesMap[sourceKey]
            val remoteSourcePreference = remotePreferencesMap[sourceKey]

            logcat(LogPriority.DEBUG, logTag) {
                "Processing source preference key: $sourceKey. " +
                    "Local source preference: ${localSourcePreference != null}, " +
                    "Remote source preference: ${remoteSourcePreference != null}"
            }

            when {
                localSourcePreference != null && remoteSourcePreference == null -> {
                    logcat(LogPriority.DEBUG, logTag) {
                        "Using local source preference: ${localSourcePreference.sourceKey}."
                    }
                    localSourcePreference
                }
                remoteSourcePreference != null && localSourcePreference == null -> {
                    logcat(LogPriority.DEBUG, logTag) {
                        "Using remote source preference: ${remoteSourcePreference.sourceKey}."
                    }
                    remoteSourcePreference
                }
                localSourcePreference != null && remoteSourcePreference != null -> {
                    // Merge the individual preferences within the source preferences
                    val mergedPrefs =
                        mergeIndividualPreferences(localSourcePreference.prefs, remoteSourcePreference.prefs)
                    BackupSourcePreferences(sourceKey, mergedPrefs)
                }
                else -> {
                    null
                }
            }
        }

    logcat(LogPriority.DEBUG, logTag) {
        "Source preferences merge completed. Total merged source preferences: ${mergedSourcePreferences.size}"
    }

    return mergedSourcePreferences
}

internal fun SyncService.mergeIndividualPreferences(
    localPrefs: List<BackupPreference>,
    remotePrefs: List<BackupPreference>,
): List<BackupPreference> {
    val mergedPrefsMap = (localPrefs + remotePrefs).associateBy { it.key }
    return mergedPrefsMap.values.toList()
}

// SY -->
internal fun SyncService.mergeSavedSearchesLists(
    localSearches: List<BackupSavedSearch>?,
    remoteSearches: List<BackupSavedSearch>?,
): List<BackupSavedSearch> {
    val logTag = "MergeSavedSearches"

    // Define a function to create a composite key from a BackupSavedSearch
    fun searchCompositeKey(search: BackupSavedSearch): String = "${search.name}|${search.source}"

    // Create maps using the composite key
    val localSearchMap = localSearches?.associateBy { searchCompositeKey(it) } ?: emptyMap()
    val remoteSearchMap = remoteSearches?.associateBy { searchCompositeKey(it) } ?: emptyMap()

    logcat(LogPriority.DEBUG, logTag) {
        "Starting saved searches merge. Local saved searches: ${localSearches?.size}, " +
            "Remote saved searches: ${remoteSearches?.size}"
    }

    // Merge both saved searches maps
    val mergedSearches = (localSearchMap.keys + remoteSearchMap.keys).distinct().mapNotNull { compositeKey ->
        val localSearch = localSearchMap[compositeKey]
        val remoteSearch = remoteSearchMap[compositeKey]

        logcat(LogPriority.DEBUG, logTag) {
            "Processing saved search key: $compositeKey. Local search: ${localSearch != null}, " +
                "Remote search: ${remoteSearch != null}"
        }

        when {
            localSearch != null && remoteSearch == null -> {
                logcat(LogPriority.DEBUG, logTag) { "Using local saved search: ${localSearch.name}." }
                localSearch
            }
            remoteSearch != null && localSearch == null -> {
                logcat(LogPriority.DEBUG, logTag) { "Using remote saved search: ${remoteSearch.name}." }
                remoteSearch
            }

            else -> {
                logcat(
                    LogPriority.DEBUG,
                    logTag,
                ) { "Both remote and local have the same saved search key: $compositeKey. Keeping local." }
                localSearch
            }
        }
    }

    logcat(LogPriority.DEBUG, logTag) {
        "Saved searches merge completed. Total merged saved searches: ${mergedSearches.size}"
    }

    return mergedSearches
}
