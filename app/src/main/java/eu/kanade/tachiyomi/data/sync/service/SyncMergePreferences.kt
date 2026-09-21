package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import logcat.LogPriority
import logcat.logcat

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
