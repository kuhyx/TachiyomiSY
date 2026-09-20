package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.sync.service.SyncService.Companion.MAX_REMOTE_DROP_RATIO
import logcat.LogPriority
import logcat.logcat
import kotlin.time.Duration.Companion.milliseconds

private const val MANGA_LOG_TAG = "MergeMangaLists"
private const val CHAPTER_LOG_TAG = "MergeChapters"

/** Re-points a manga's category orders from one side's numbering onto the merged category list. */
private class CategoryOrderRemap(
    localCategories: List<BackupCategory>,
    remoteCategories: List<BackupCategory>,
    mergedCategories: List<BackupCategory>,
) {
    private val localByOrder = localCategories.associateBy { it.order }
    private val remoteByOrder = remoteCategories.associateBy { it.order }
    private val mergedByName = mergedCategories.associateBy { it.name }

    fun applyLocal(manga: BackupManga) = remap(manga, localByOrder)
    fun applyRemote(manga: BackupManga) = remap(manga, remoteByOrder)

    private fun remap(manga: BackupManga, byOrder: Map<Long, BackupCategory>) {
        manga.categories = manga.categories.mapNotNull { byOrder[it]?.let { c -> mergedByName[c.name]?.order } }
    }
}

private class MangaMergeContext(
    val lastSyncTime: Long,
    val syncingChapters: Boolean,
    val categories: CategoryOrderRemap,
) {
    // Remote entries discarded because they look locally deleted. Deleting a manga
    // in the app does not remove its row -- it clears `favorite` and stamps
    // `favorite_modified_at` -- so a real deletion still appears in the local list
    // and takes the local/remote branch. A row that is missing outright means
    // the local database lost it, which is what a failed restore does.
    var droppedAsDeletedRemotely = 0

    fun modifiedSinceSync(manga: BackupManga): Boolean = lastSyncTime == 0L || manga.lastModifiedAt > lastSyncTime
}

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
    val localMangaListSafe = localMangaList.orEmpty()
    val remoteMangaListSafe = remoteMangaList.orEmpty()
    logcat(MANGA_LOG_TAG, LogPriority.DEBUG) {
        "Starting merge. Local list size: ${localMangaListSafe.size}, Remote list size: ${remoteMangaListSafe.size}"
    }

    fun mangaCompositeKey(manga: BackupManga): String = "${manga.source}|${manga.url}"
    val localMangaMap = localMangaListSafe.associateBy { mangaCompositeKey(it) }
    val remoteMangaMap = remoteMangaListSafe.associateBy { mangaCompositeKey(it) }

    val context = MangaMergeContext(
        lastSyncTime = syncPreferences.lastSyncTimestamp.get().milliseconds.inWholeSeconds,
        syncingChapters = syncPreferences.getSyncSettings().chapters,
        categories = CategoryOrderRemap(localCategories, remoteCategories, mergedCategories),
    )
    val mergedList = (localMangaMap.keys + remoteMangaMap.keys).distinct().mapNotNull { compositeKey ->
        mergeManga(localMangaMap[compositeKey], remoteMangaMap[compositeKey], context)
    }

    if (remoteMangaListSafe.isNotEmpty() &&
        context.droppedAsDeletedRemotely > remoteMangaListSafe.size * MAX_REMOTE_DROP_RATIO
    ) {
        throw SyncCollapseException(
            "Refusing to sync: ${context.droppedAsDeletedRemotely} of ${remoteMangaListSafe.size} server entries " +
                "are missing from this device. That is a damaged local library, not a deletion -- " +
                "restore this device from a backup before syncing again.",
        )
    }

    val (favorites, nonFavorites) = mergedList.partition { it.favorite }
    logcat(MANGA_LOG_TAG, LogPriority.DEBUG) {
        "Merge completed. Total merged manga: ${mergedList.size}, Favorites: ${favorites.size}, " +
            "Non-Favorites: ${nonFavorites.size}"
    }
    return mergedList
}

private fun mergeManga(local: BackupManga?, remote: BackupManga?, context: MangaMergeContext): BackupManga? = when {
    local != null && remote == null -> localOnlyManga(local, context)
    local == null && remote != null -> remoteOnlyManga(remote, context)
    local != null && remote != null -> newerManga(local, remote, context)
    else -> null // No manga found for key
}

private fun localOnlyManga(local: BackupManga, context: MangaMergeContext): BackupManga? {
    if (!context.modifiedSinceSync(local)) {
        logcat(MANGA_LOG_TAG, LogPriority.DEBUG) { "Dropping local manga deleted on remote: ${local.title}." }
        return null
    }
    context.categories.applyLocal(local)
    return local
}

private fun remoteOnlyManga(remote: BackupManga, context: MangaMergeContext): BackupManga? {
    if (!context.modifiedSinceSync(remote)) {
        // Absent locally and untouched on the server since our last sync, so the user deleted
        // it here. That reading only holds while the local database is intact -- see
        // [MangaMergeContext.droppedAsDeletedRemotely].
        context.droppedAsDeletedRemotely++
        logcat(MANGA_LOG_TAG, LogPriority.DEBUG) { "Dropping deleted remote manga: ${remote.title}." }
        return null
    }
    context.categories.applyRemote(remote)
    return remote
}

// Compare versions to decide which manga to keep; either way the chapters are merged.
private fun newerManga(local: BackupManga, remote: BackupManga, context: MangaMergeContext): BackupManga {
    val chapters = mergeChapters(local.chapters, remote.chapters, context.lastSyncTime, context.syncingChapters)
    return if (local.version >= remote.version) {
        logcat(MANGA_LOG_TAG, LogPriority.DEBUG) { "Keeping local version of ${local.title} with merged chapters." }
        local.chapters = chapters
        context.categories.applyLocal(local)
        local
    } else {
        logcat(MANGA_LOG_TAG, LogPriority.DEBUG) { "Keeping remote version of ${remote.title} with merged chapters." }
        remote.chapters = chapters
        context.categories.applyRemote(remote)
        remote
    }
}

internal fun mergeChapters(
    localChapters: List<BackupChapter>,
    remoteChapters: List<BackupChapter>,
    lastSyncTime: Long,
    syncingChapters: Boolean,
): List<BackupChapter> {
    if (!syncingChapters) {
        return remoteChapters // If not syncing chapters, keep remote untouched
    }
    val localChapterMap = localChapters.associateBy { it.url }
    val remoteChapterMap = remoteChapters.associateBy { it.url }
    logcat(CHAPTER_LOG_TAG, LogPriority.DEBUG) {
        "Starting chapter merge. Local chapters: ${localChapters.size}, Remote chapters: ${remoteChapters.size}"
    }
    // If there are more chapters on remote, local sourceOrder will need to be updated to maintain
    // correct source order.
    val takeRemoteOrder = localChapters.size < remoteChapters.size

    // Merge both chapter maps based on version numbers
    val mergedChapters = (localChapterMap.keys + remoteChapterMap.keys).distinct().mapNotNull { compositeKey ->
        val localChapter = localChapterMap[compositeKey]
        val remoteChapter = remoteChapterMap[compositeKey]
        logcat(CHAPTER_LOG_TAG, LogPriority.DEBUG) {
            "Processing chapter key: $compositeKey. Local chapter: ${localChapter != null}, " +
                "Remote chapter: ${remoteChapter != null}"
        }
        when {
            localChapter != null && remoteChapter == null -> {
                localOnlyChapter(localChapter, lastSyncTime)
            }
            localChapter == null && remoteChapter != null -> {
                remoteOnlyChapter(remoteChapter, lastSyncTime)
            }
            localChapter != null && remoteChapter != null -> {
                newerChapter(localChapter, remoteChapter, takeRemoteOrder)
            }
            else -> {
                logcat(
                    CHAPTER_LOG_TAG,
                    LogPriority.DEBUG,
                ) { "No chapter found for composite key: $compositeKey. Skipping." }
                null
            }
        }
    }
    logcat(
        CHAPTER_LOG_TAG,
        LogPriority.DEBUG,
    ) { "Chapter merge completed. Total merged chapters: ${mergedChapters.size}" }
    return mergedChapters
}

private fun localOnlyChapter(localChapter: BackupChapter, lastSyncTime: Long): BackupChapter? {
    return if (lastSyncTime == 0L || localChapter.lastModifiedAt > lastSyncTime) {
        logcat(CHAPTER_LOG_TAG, LogPriority.DEBUG) { "Keeping local chapter: ${localChapter.name}." }
        localChapter
    } else {
        logcat(CHAPTER_LOG_TAG, LogPriority.DEBUG) { "Dropping local chapter deleted on remote: ${localChapter.name}." }
        null
    }
}

private fun remoteOnlyChapter(remoteChapter: BackupChapter, lastSyncTime: Long): BackupChapter? {
    return if (lastSyncTime == 0L || remoteChapter.lastModifiedAt > lastSyncTime) {
        logcat(CHAPTER_LOG_TAG, LogPriority.DEBUG) { "Taking remote chapter: ${remoteChapter.name}." }
        remoteChapter
    } else {
        logcat(CHAPTER_LOG_TAG, LogPriority.DEBUG) { "Dropping deleted remote chapter: ${remoteChapter.name}." }
        null
    }
}

// Use version number to decide which chapter to keep.
private fun newerChapter(
    localChapter: BackupChapter,
    remoteChapter: BackupChapter,
    takeRemoteOrder: Boolean,
): BackupChapter {
    val keepLocal = localChapter.version >= remoteChapter.version
    val chosenChapter = if (keepLocal) {
        if (takeRemoteOrder) localChapter.sourceOrder = remoteChapter.sourceOrder
        localChapter
    } else {
        remoteChapter
    }
    logcat(CHAPTER_LOG_TAG, LogPriority.DEBUG) {
        "Merging chapter: ${chosenChapter.name}. Chosen version from: ${if (keepLocal) "Local" else "Remote"}, " +
            "Local version: ${localChapter.version}, Remote version: ${remoteChapter.version}."
    }
    return chosenChapter
}
