package eu.kanade.tachiyomi.data.backup.restore.restorers

import app.cash.sqldelight.async.coroutines.awaitAsOne
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import tachiyomi.domain.chapter.model.copyFrom
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

internal suspend fun MangaRestorer.findExistingManga(backupManga: BackupManga): Manga? =
    getMangaByUrlAndSourceId.await(backupManga.url, backupManga.source)

internal suspend fun MangaRestorer.restoreExistingManga(manga: Manga, dbManga: Manga): Manga {
    return if (manga.version > dbManga.version) {
        persistManga(dbManga.copyFrom(manga).copy(id = dbManga.id))
    } else {
        persistManga(manga.copyFrom(dbManga).copy(id = dbManga.id))
    }
}

internal suspend fun MangaRestorer.persistManga(manga: Manga): Manga {
    database.mangasQueries.update(
        source = manga.source,
        url = manga.url,
        // SY -->
        artist = manga.ogArtist,
        author = manga.ogAuthor,
        description = manga.ogDescription,
        genre = manga.ogGenre,
        title = manga.ogTitle,
        status = manga.ogStatus,
        thumbnailUrl = manga.ogThumbnailUrl,
        // SY <--
        favorite = manga.favorite,
        lastUpdate = manga.lastUpdate,
        nextUpdate = null,
        calculateInterval = null,
        initialized = manga.initialized,
        viewer = manga.viewerFlags,
        chapterFlags = manga.chapterFlags,
        coverLastModified = manga.coverLastModified,
        dateAdded = manga.dateAdded,
        mangaId = manga.id,
        updateStrategy = manga.updateStrategy,
        version = manga.version,
        isSyncing = 1,
        notes = manga.notes,
        memo = manga.memo,
    )
    return manga
}

internal suspend fun MangaRestorer.restoreNewManga(
    manga: Manga,
): Manga {
    return manga.copy(
        id = insertManga(manga),
    )
}

// Inserts manga and returns id.
// @return id of [Manga], null if not found
internal suspend fun MangaRestorer.insertManga(manga: Manga): Long {
    return database.mangasQueries.insertReturningId(
        source = manga.source,
        url = manga.url,
        // SY -->
        artist = manga.ogArtist,
        author = manga.ogAuthor,
        description = manga.ogDescription,
        genre = manga.ogGenre,
        title = manga.ogTitle,
        status = manga.ogStatus,
        thumbnailUrl = manga.ogThumbnailUrl,
        // SY <--
        favorite = manga.favorite,
        lastUpdate = manga.lastUpdate,
        nextUpdate = 0L,
        calculateInterval = 0L,
        initialized = manga.initialized,
        viewerFlags = manga.viewerFlags,
        chapterFlags = manga.chapterFlags,
        coverLastModified = manga.coverLastModified,
        dateAdded = manga.dateAdded,
        updateStrategy = manga.updateStrategy,
        version = manga.version,
        notes = manga.notes,
        memo = manga.memo,
    )
        .awaitAsOne()
}
