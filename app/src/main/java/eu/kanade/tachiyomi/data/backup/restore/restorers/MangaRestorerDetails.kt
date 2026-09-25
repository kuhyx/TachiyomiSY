package eu.kanade.tachiyomi.data.backup.restore.restorers

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import eu.kanade.tachiyomi.data.backup.models.getHistoryImpl
import eu.kanade.tachiyomi.data.backup.models.getTrackImpl
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get
import java.util.Date
import kotlin.math.max

// Restores everything the backup entry carries besides the manga row itself.
internal suspend fun MangaRestorer.restoreMangaDetails(
    manga: Manga,
    backupManga: BackupManga,
    backupCategories: List<BackupCategory>,
): Manga {
    restoreCategories(manga, backupManga.categories, backupCategories)
    restoreChapters(manga, backupManga.chapters)
    restoreTracking(manga, backupManga.tracking)
    restoreHistory(manga, backupManga.history)
    restoreExcludedScanlators(manga, backupManga.excludedScanlators)
    updateManga.awaitUpdateFetchInterval(manga, now, currentFetchWindow)
    // SY -->
    restoreMergedReferencesFor(manga.id, backupManga.mergedMangaReferences)
    backupManga.flatMetadata?.let { restoreFlatMetadata(manga.id, it) }
    restoreEditedInfo(backupManga.getCustomMangaInfo()?.copy(id = manga.id))
    // SY <--

    return manga
}

// Restores the categories a manga is in.
// @param manga the manga whose categories have to be restored.
// @param categories the categories to restore.
internal suspend fun MangaRestorer.restoreCategories(
    manga: Manga,
    categories: List<Long>,
    backupCategories: List<BackupCategory>,
) {
    val dbCategories = getCategories.await()
    val dbCategoriesByName = dbCategories.associateBy { it.name }

    val backupCategoriesByOrder = backupCategories.associateBy { it.order }

    val mangaCategoriesToUpdate = categories.mapNotNull { backupCategoryOrder ->
        backupCategoriesByOrder[backupCategoryOrder]?.let { backupCategory ->
            dbCategoriesByName[backupCategory.name]?.let { dbCategory ->
                Pair(manga.id, dbCategory.id)
            }
        }
    }

    if (mangaCategoriesToUpdate.isNotEmpty()) {
        database.transaction {
            database.mangas_categoriesQueries.deleteMangaCategoryByMangaId(manga.id)
            mangaCategoriesToUpdate.forEach { (mangaId, categoryId) ->
                database.mangas_categoriesQueries.insert(mangaId, categoryId)
            }
        }
    }
}

internal suspend fun MangaRestorer.restoreHistory(manga: Manga, backupHistory: List<BackupHistory>) {
    val toUpdate = backupHistory.mapNotNull { history ->
        val dbHistory = database.historyQueries
            .getHistoryByChapterUrl(manga.id, history.url)
            .awaitAsOneOrNull()
        val item = history.getHistoryImpl()

        if (dbHistory == null) {
            val chapter = database.chaptersQueries
                .getChapterByUrl(history.url)
                .awaitAsList()
                .find { it.manga_id == manga.id }
            // No chapter means the entry is skipped; otherwise it becomes a new history entry.
            chapter?.let { item.copy(chapterId = it._id) }
        } else {
            // Update history entry
            item.copy(
                id = dbHistory._id,
                chapterId = dbHistory.chapter_id,
                readAt = max(history.lastRead, dbHistory.last_read?.time ?: 0L)
                    .takeIf { it > 0L }
                    ?.let { Date(it) },
                readDuration = max(item.readDuration, dbHistory.time_read) - dbHistory.time_read,
            )
        }
    }

    if (toUpdate.isEmpty()) return
    database.transaction {
        toUpdate.forEach {
            database.historyQueries.upsert(
                it.chapterId,
                it.readAt,
                it.readDuration,
            )
        }
    }
}

internal suspend fun MangaRestorer.restoreTracking(manga: Manga, backupTracks: List<BackupTracking>) {
    val dbTrackByTrackerId = getTracks.await(manga.id).associateBy { it.trackerId }

    val (existingTracks, newTracks) = backupTracks
        .mapNotNull {
            val track = it.getTrackImpl()
            val dbTrack = dbTrackByTrackerId[track.trackerId]
            when {
                // New track; the db assigns the id
                dbTrack == null -> track.copy(id = 0, mangaId = manga.id)
                // Same state; skip
                track.forComparison() == dbTrack.forComparison() -> null
                // Update to an existing track
                else -> dbTrack.copy(
                    remoteId = track.remoteId,
                    libraryId = track.libraryId,
                    lastChapterRead = max(dbTrack.lastChapterRead, track.lastChapterRead),
                )
            }
        }
        .partition { it.id > 0 }

    if (newTracks.isNotEmpty()) {
        insertTrack.awaitAll(newTracks)
    }

    if (existingTracks.isEmpty()) return
    database.transaction {
        existingTracks.forEach { track ->
            database.manga_syncQueries.update(
                track.mangaId,
                track.trackerId,
                track.remoteId,
                track.libraryId,
                track.title,
                track.lastChapterRead,
                track.totalChapters,
                track.status,
                track.score,
                track.remoteUrl,
                track.startDate,
                track.finishDate,
                track.private,
                track.id,
            )
        }
    }
}

// Restores the excluded scanlators for the manga.
// @param manga the manga whose excluded scanlators have to be restored.
// @param excludedScanlators the excluded scanlators to restore.
internal suspend fun MangaRestorer.restoreExcludedScanlators(manga: Manga, excludedScanlators: List<String>) {
    if (excludedScanlators.isEmpty()) return
    val existingExcludedScanlators = database.excluded_scanlatorsQueries
        .getExcludedScanlatorsByMangaId(manga.id)
        .awaitAsList()
    val toInsert = excludedScanlators.filter { it !in existingExcludedScanlators }
    if (toInsert.isEmpty()) return
    toInsert.forEach { database.excluded_scanlatorsQueries.insert(manga.id, it) }
}
