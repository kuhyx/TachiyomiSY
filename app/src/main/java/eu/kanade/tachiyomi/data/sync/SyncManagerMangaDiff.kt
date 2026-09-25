package eu.kanade.tachiyomi.data.sync

import app.cash.sqldelight.async.coroutines.awaitAsList
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.restore.restorers.persistManga
import logcat.LogPriority
import logcat.logcat
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.awaitList
import tachiyomi.data.manga.MangaMapper.mapManga
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get
import kotlin.system.measureTimeMillis

private const val MILLIS_PER_SECOND = 1000L
private const val MILLIS_PER_MINUTE = 60L * MILLIS_PER_SECOND

// Retrieves all manga from the local database.
// @return a list of all manga stored in the database
internal suspend fun SyncManager.getAllMangaFromDB(): List<Manga> {
    return database.mangasQueries
        .getAllManga()
        .awaitList(::mapManga)
}

internal suspend fun SyncManager.getAllMangaThatNeedsSync(): List<Manga> {
    return database.mangasQueries
        .getMangasWithFavoriteTimestamp()
        .awaitList(::mapManga)
}

internal suspend fun SyncManager.isMangaDifferent(localManga: Manga, remoteManga: BackupManga): Boolean {
    val localChapters = database.chaptersQueries.getChaptersByMangaId(localManga.id, 0).awaitAsList()
    val localCategories = getCategories.await(localManga.id).map { it.order }

    return areChaptersDifferent(localChapters, remoteManga.chapters) ||
        localManga.version != remoteManga.version ||
        localCategories.toSet() != remoteManga.categories.toSet()
}

// Filters the favorite and non-favorite manga from the backup and checks
// if the favorite manga is different from the local database.
// @param backup the Backup object containing the backup data.
// @return a Pair of lists, where the first list contains different favorite manga
// and the second list contains non-favorite manga.
internal suspend fun SyncManager.filterFavoritesAndNonFavorites(
    backup: Backup,
): Pair<List<BackupManga>, List<BackupManga>> {
    val favorites = mutableListOf<BackupManga>()
    val nonFavorites = mutableListOf<BackupManga>()
    val logTag = "filterFavoritesAndNonFavorites"

    val elapsedTimeMillis = measureTimeMillis {
        val databaseManga = getAllMangaFromDB()
        val localMangaMap = databaseManga.associateBy {
            Pair(it.source, it.url)
        }

        logcat(LogPriority.DEBUG, logTag) { "Starting to filter favorites and non-favorites from backup data." }

        backup.backupManga.forEach { remoteManga ->
            val compositeKey = Pair(remoteManga.source, remoteManga.url)
            val localManga = localMangaMap[compositeKey]
            when {
                // Checks if the manga is in favorites and needs updating or adding
                remoteManga.favorite -> {
                    if (localManga == null || isMangaDifferent(localManga, remoteManga)) {
                        logcat(LogPriority.DEBUG, logTag) { "Adding to favorites: ${remoteManga.title}" }
                        favorites.add(remoteManga)
                    } else {
                        logcat(LogPriority.DEBUG, logTag) { "Already up-to-date favorite: ${remoteManga.title}" }
                    }
                }
                // Handle non-favorites
                else -> {
                    logcat(LogPriority.DEBUG, logTag) { "Adding to non-favorites: ${remoteManga.title}" }
                    nonFavorites.add(remoteManga)
                }
            }
        }
    }

    val minutes = elapsedTimeMillis / MILLIS_PER_MINUTE
    val seconds = elapsedTimeMillis % MILLIS_PER_MINUTE / MILLIS_PER_SECOND
    logcat(LogPriority.DEBUG, logTag) {
        "Filtering completed in ${minutes}m ${seconds}s. Favorites found: ${favorites.size}, " +
            "Non-favorites found: ${nonFavorites.size}"
    }

    return Pair(favorites, nonFavorites)
}

// Updates the non-favorite manga in the local database with their favorite status from the backup.
// @param nonFavorites the list of non-favorite BackupManga objects from the backup.
internal suspend fun SyncManager.updateNonFavorites(nonFavorites: List<BackupManga>) {
    val localMangaList = getAllMangaFromDB()

    val localMangaMap = localMangaList.associateBy { Pair(it.source, it.url) }

    nonFavorites.forEach { nonFavorite ->
        val key = Pair(nonFavorite.source, nonFavorite.url)
        localMangaMap[key]?.let { localManga ->
            if (localManga.favorite != nonFavorite.favorite) {
                val updatedManga = localManga.copy(favorite = nonFavorite.favorite)
                mangaRestorer.persistManga(updatedManga)
            }
        }
    }
}
