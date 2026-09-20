package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupFlatMetadata
import eu.kanade.tachiyomi.data.backup.models.BackupMergedMangaReference
import eu.kanade.tachiyomi.data.backup.models.getFlatMetadata
import eu.kanade.tachiyomi.data.backup.models.getMergedMangaReference
import tachiyomi.data.awaitList
import tachiyomi.data.awaitOneOrNull
import tachiyomi.data.manga.MangaMapper
import tachiyomi.data.manga.MergedMangaMapper
import tachiyomi.domain.manga.model.CustomMangaInfo
import uy.kohesive.injekt.api.get

// Restore the categories from Json.
// @param manga the merge manga for the references
// @param backupMergedMangaReferences the list of backup manga references for the merged manga
internal suspend fun MangaRestorer.restoreMergedReferencesFor(
    mergeMangaId: Long,
    backupMergedMangaReferences: List<BackupMergedMangaReference>,
) {
    // Get merged manga references from file and from db
    val dbMergedMangaReferences =
        database.mergedQueries.selectAll()
            .awaitList(MergedMangaMapper::map)

    // Iterate over them
    backupMergedMangaReferences.forEach { backupMergedMangaReference ->
        // If the backupMergedMangaReference isn't in the db,
        // remove the id and insert a new backupMergedMangaReference
        // Store the inserted id in the backupMergedMangaReference
        if (dbMergedMangaReferences.none {
                backupMergedMangaReference.mergeUrl == it.mergeUrl &&
                    backupMergedMangaReference.mangaUrl == it.mangaUrl
            }
        ) {
            // Let the db assign the id
            val mergedManga = database.mangasQueries.getMangaByUrlAndSource(
                backupMergedMangaReference.mangaUrl,
                backupMergedMangaReference.mangaSourceId,
            )
                .awaitOneOrNull(MangaMapper::mapManga)
                ?: return@forEach
            backupMergedMangaReference.getMergedMangaReference().run {
                database.mergedQueries.insert(
                    infoManga = isInfoManga,
                    getChapterUpdates = getChapterUpdates,
                    chapterSortMode = chapterSortMode.toLong(),
                    chapterPriority = chapterPriority.toLong(),
                    downloadChapters = downloadChapters,
                    mergeId = mergeMangaId,
                    mergeUrl = mergeUrl,
                    mangaId = mergedManga.id,
                    mangaUrl = mangaUrl,
                    mangaSource = mangaSourceId,
                )
            }
        }
    }
}

internal suspend fun MangaRestorer.restoreFlatMetadata(mangaId: Long, backupFlatMetadata: BackupFlatMetadata) {
    if (getFlatMetadataById.await(mangaId) == null) {
        insertFlatMetadata.await(backupFlatMetadata.getFlatMetadata(mangaId))
    }
}

internal fun MangaRestorer.restoreEditedInfo(mangaJson: CustomMangaInfo?) {
    mangaJson ?: return
    setCustomMangaInfo.set(mangaJson)
}
