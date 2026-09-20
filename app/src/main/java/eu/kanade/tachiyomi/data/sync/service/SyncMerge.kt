package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.sync.service.SyncService.Companion.MAX_REMOTE_DROP_RATIO
import logcat.LogPriority
import logcat.logcat
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Merges two lists of BackupManga objects, selecting the most recent manga based on the lastModifiedAt value.
// If lastModifiedAt is null for a manga, it treats that manga as the oldest possible for comparison purposes.
// This function is designed to reconcile local and remote manga lists, ensuring the most up-to-date manga is
// retained.
// @param localMangaList The list of local BackupManga objects or null.
// @param remoteMangaList The list of remote BackupManga objects or null.
// @return A list of BackupManga objects, each representing the most recent version of the manga from either local
// or remote sources.
internal fun SyncService.mergeMangaLists(
    localMangaList: List<BackupManga>?,
    remoteMangaList: List<BackupManga>?,
    localCategories: List<BackupCategory>,
    remoteCategories: List<BackupCategory>,
    mergedCategories: List<BackupCategory>,
): List<BackupManga> {
    val logTag = "MergeMangaLists"

    val localMangaListSafe = localMangaList.orEmpty()
    val remoteMangaListSafe = remoteMangaList.orEmpty()

    logcat(LogPriority.DEBUG, logTag) {
        "Starting merge. Local list size: ${localMangaListSafe.size}, Remote list size: ${remoteMangaListSafe.size}"
    }

    fun mangaCompositeKey(manga: BackupManga): String = "${manga.source}|${manga.url}"

    // Create maps using composite keys
    val localMangaMap = localMangaListSafe.associateBy { mangaCompositeKey(it) }
    val remoteMangaMap = remoteMangaListSafe.associateBy { mangaCompositeKey(it) }

    val localCategoriesMapByOrder = localCategories.associateBy { it.order }
    val remoteCategoriesMapByOrder = remoteCategories.associateBy { it.order }
    val mergedCategoriesMapByName = mergedCategories.associateBy { it.name }

    fun updateCategories(theManga: BackupManga, theMap: Map<Long, BackupCategory>) {
        theManga.categories = theManga.categories.mapNotNull {
            theMap[it]?.let { category ->
                mergedCategoriesMapByName[category.name]?.order
            }
        }
    }

    logcat(LogPriority.DEBUG, logTag) {
        "Starting merge. Local list size: ${localMangaListSafe.size}, Remote list size: ${remoteMangaListSafe.size}"
    }

    val lastSyncTime = syncPreferences.lastSyncTimestamp.get().milliseconds.inWholeSeconds
    val syncOptions = syncPreferences.getSyncSettings()

    // Remote entries discarded because they look locally deleted. Deleting a manga
    // in the app does not remove its row -- it clears `favorite` and stamps
    // `favorite_modified_at` -- so a real deletion still appears in the local list
    // and takes the local/remote branch below. A row that is missing outright means
    // the local database lost it, which is what a failed restore does.
    var droppedAsDeletedRemotely = 0

    val mergedList = (localMangaMap.keys + remoteMangaMap.keys).distinct().mapNotNull { compositeKey ->
        val local = localMangaMap[compositeKey]
        val remote = remoteMangaMap[compositeKey]

        // New version comparison logic
        when {
            local != null && remote == null -> {
                if (lastSyncTime == 0L || local.lastModifiedAt > lastSyncTime) {
                    updateCategories(local, localCategoriesMapByOrder)
                    local
                } else {
                    logcat(LogPriority.DEBUG, logTag) { "Dropping local manga deleted on remote: ${local.title}." }
                    null
                }
            }
            local == null && remote != null -> {
                if (lastSyncTime == 0L || remote.lastModifiedAt > lastSyncTime) {
                    updateCategories(remote, remoteCategoriesMapByOrder)
                    remote
                } else {
                    // Absent locally and untouched on the server since our last sync,
                    // so the user deleted it here. That reading only holds while the
                    // local database is intact -- see droppedAsDeletedRemotely below.
                    droppedAsDeletedRemotely++
                    logcat(LogPriority.DEBUG, logTag) { "Dropping deleted remote manga: ${remote.title}." }
                    null
                }
            }
            local != null && remote != null -> {
                // Compare versions to decide which manga to keep
                if (local.version >= remote.version) {
                    logcat(LogPriority.DEBUG, logTag) {
                        "Keeping local version of ${local.title} with merged chapters."
                    }
                    local.chapters =
                        mergeChapters(local.chapters, remote.chapters, lastSyncTime, syncOptions.chapters)
                    updateCategories(local, localCategoriesMapByOrder)
                    local
                } else {
                    logcat(LogPriority.DEBUG, logTag) {
                        "Keeping remote version of ${remote.title} with merged chapters."
                    }
                    remote.chapters =
                        mergeChapters(local.chapters, remote.chapters, lastSyncTime, syncOptions.chapters)
                    updateCategories(remote, remoteCategoriesMapByOrder)
                    remote
                }
            }
            else -> {
                null // No manga found for key
            }
        }
    }

    if (remoteMangaListSafe.isNotEmpty() &&
        droppedAsDeletedRemotely > remoteMangaListSafe.size * MAX_REMOTE_DROP_RATIO
    ) {
        throw SyncCollapseException(
            "Refusing to sync: $droppedAsDeletedRemotely of ${remoteMangaListSafe.size} server entries " +
                "are missing from this device. That is a damaged local library, not a deletion -- " +
                "restore this device from a backup before syncing again.",
        )
    }

    // Counting favorites and non-favorites
    val (favorites, nonFavorites) = mergedList.partition { it.favorite }

    logcat(LogPriority.DEBUG, logTag) {
        "Merge completed. Total merged manga: ${mergedList.size}, Favorites: ${favorites.size}, " +
            "Non-Favorites: ${nonFavorites.size}"
    }

    return mergedList
}

internal fun SyncService.mergeChapters(
    localChapters: List<BackupChapter>,
    remoteChapters: List<BackupChapter>,
    lastSyncTime: Long,
    syncingChapters: Boolean,
): List<BackupChapter> {
    val logTag = "MergeChapters"

    if (!syncingChapters) {
        return remoteChapters // If not syncing chapters, keep remote untouched
    }

    fun chapterCompositeKey(chapter: BackupChapter): String = chapter.url

    val localChapterMap = localChapters.associateBy { chapterCompositeKey(it) }
    val remoteChapterMap = remoteChapters.associateBy { chapterCompositeKey(it) }

    logcat(LogPriority.DEBUG, logTag) {
        "Starting chapter merge. Local chapters: ${localChapters.size}, Remote chapters: ${remoteChapters.size}"
    }

    // Merge both chapter maps based on version numbers
    val mergedChapters = (localChapterMap.keys + remoteChapterMap.keys).distinct().mapNotNull { compositeKey ->
        val localChapter = localChapterMap[compositeKey]
        val remoteChapter = remoteChapterMap[compositeKey]

        logcat(LogPriority.DEBUG, logTag) {
            "Processing chapter key: $compositeKey. Local chapter: ${localChapter != null}, " +
                "Remote chapter: ${remoteChapter != null}"
        }

        when {
            localChapter != null && remoteChapter == null -> {
                if (lastSyncTime == 0L || localChapter.lastModifiedAt > lastSyncTime) {
                    logcat(LogPriority.DEBUG, logTag) { "Keeping local chapter: ${localChapter.name}." }
                    localChapter
                } else {
                    logcat(
                        LogPriority.DEBUG,
                        logTag,
                    ) { "Dropping local chapter deleted on remote: ${localChapter.name}." }
                    null
                }
            }
            localChapter == null && remoteChapter != null -> {
                if (lastSyncTime == 0L || remoteChapter.lastModifiedAt > lastSyncTime) {
                    logcat(LogPriority.DEBUG, logTag) { "Taking remote chapter: ${remoteChapter.name}." }
                    remoteChapter
                } else {
                    logcat(LogPriority.DEBUG, logTag) { "Dropping deleted remote chapter: ${remoteChapter.name}." }
                    null
                }
            }
            localChapter != null && remoteChapter != null -> {
                // Use version number to decide which chapter to keep
                val chosenChapter = if (localChapter.version >= remoteChapter.version) {
                    // If there mare more chapter on remote, local sourceOrder will need to be updated to maintain
                    // correct source order.
                    if (localChapters.size < remoteChapters.size) {
                        localChapter.sourceOrder = remoteChapter.sourceOrder
                        localChapter
                    } else {
                        localChapter
                    }
                } else {
                    remoteChapter
                }
                logcat(LogPriority.DEBUG, logTag) {
                    "Merging chapter: ${chosenChapter.name}. Chosen version from: ${
                        if (localChapter.version >= remoteChapter.version) "Local" else "Remote"
                    }, Local version: ${localChapter.version}, Remote version: ${remoteChapter.version}."
                }
                chosenChapter
            }
            else -> {
                logcat(LogPriority.DEBUG, logTag) {
                    "No chapter found for composite key: $compositeKey. Skipping."
                }
                null
            }
        }
    }

    logcat(LogPriority.DEBUG, logTag) { "Chapter merge completed. Total merged chapters: ${mergedChapters.size}" }

    return mergedChapters
}

// Merges two lists of SyncCategory objects, prioritizing the category with the most recent order value.
// @param localCategoriesList The list of local SyncCategory objects.
// @param remoteCategoriesList The list of remote SyncCategory objects.
// @return The merged list of SyncCategory objects.
internal fun SyncService.mergeCategoriesLists(
    localCategoriesList: List<BackupCategory>?,
    remoteCategoriesList: List<BackupCategory>?,
): List<BackupCategory> {
    val logTag = "MergeCategories"
    if (localCategoriesList == null) return remoteCategoriesList ?: emptyList()
    if (remoteCategoriesList == null) return localCategoriesList

    val result = mutableListOf<BackupCategory>()
    val processedLocals = mutableSetOf<BackupCategory>()

    val localMapByUid = localCategoriesList.filter { it.uid != 0L }.associateBy { it.uid }
    val localMapByName = localCategoriesList.associateBy { it.name }

    val lastSyncTime = syncPreferences.lastSyncTimestamp.get()

    remoteCategoriesList.forEach { remote ->
        var localMatch: BackupCategory? = null

        // 1. Try match by UID
        if (remote.uid != 0L) {
            localMatch = localMapByUid[remote.uid]
        }

        // 2. Try match by Name (fallback)
        if (localMatch == null) {
            localMatch = localMapByName[remote.name]
        }

        if (localMatch != null) {
            processedLocals.add(localMatch)
            // Conflict resolution
            if (localMatch.version >= remote.version) {
                logcat(LogPriority.DEBUG, logTag) {
                    "Keeping local category: ${localMatch.name} (UID: ${localMatch.uid})"
                }
                result.add(localMatch)
            } else {
                logcat(LogPriority.DEBUG, logTag) { "Keeping remote category: ${remote.name} (UID: ${remote.uid})" }
                // Preserve Local UID if Remote was 0
                if (remote.uid == 0L) {
                    remote.uid = localMatch.uid
                }
                result.add(remote)
            }
        } else {
            val remoteModifiedTimeMillis = remote.lastModifiedAt.seconds.inWholeMilliseconds
            if (lastSyncTime == 0L || remoteModifiedTimeMillis > lastSyncTime) {
                logcat(LogPriority.DEBUG, logTag) {
                    "Adding new remote category: ${remote.name} (UID: ${remote.uid})"
                }
                result.add(remote)
            } else {
                logcat(LogPriority.DEBUG, logTag) {
                    "Dropping deleted remote category: ${remote.name} (UID: ${remote.uid})"
                }
            }
        }
    }

    // Add remaining Local Categories
    localCategoriesList.forEach { local ->
        if (local !in processedLocals) {
            val localModifiedTimeMillis = local.lastModifiedAt.seconds.inWholeMilliseconds
            if (lastSyncTime == 0L || localModifiedTimeMillis > lastSyncTime) {
                logcat(LogPriority.DEBUG, logTag) {
                    "Keeping local only category: ${local.name} (UID: ${local.uid})"
                }
                result.add(local)
            } else {
                logcat(LogPriority.DEBUG, logTag) {
                    "Dropping local category deleted on remote: ${local.name} (UID: ${local.uid})"
                }
            }
        }
    }

    return result.sortedBy { it.order }
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
