package tachiyomi.data.manga

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.data.awaitOne
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaWriteRepository

/** [MangaWriteRepository] on the SQLDelight `mangas` and `mangas_categories` tables. */
internal class MangaWriteRepositoryImpl(
    private val database: Database,
) : MangaWriteRepository {

    override suspend fun resetViewerFlags(): Boolean {
        return try {
            database.mangasQueries.resetViewerFlags()
            true
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            false
        }
    }

    override suspend fun setMangaCategories(mangaId: Long, categoryIds: List<Long>) {
        database.transaction {
            database.mangas_categoriesQueries.deleteMangaCategoryByMangaId(mangaId)
            categoryIds.forEach { categoryId ->
                database.mangas_categoriesQueries.insert(mangaId, categoryId)
            }
        }
    }

    override suspend fun update(update: MangaUpdate): Boolean = updateAll(listOf(update))

    override suspend fun updateAll(mangaUpdates: List<MangaUpdate>): Boolean {
        return try {
            partialUpdate(mangaUpdates)
            true
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            false
        }
    }

    override suspend fun insertNetworkManga(manga: List<Manga>): List<Manga> {
        return database.transactionWithResult {
            manga.map {
                database.mangasQueries.insertNetworkManga(
                    source = it.source,
                    url = it.url,
                    // SY -->
                    artist = it.ogArtist,
                    author = it.ogAuthor,
                    description = it.ogDescription,
                    genre = it.ogGenre,
                    title = it.ogTitle,
                    status = it.ogStatus,
                    thumbnailUrl = it.ogThumbnailUrl,
                    // SY <--
                    favorite = it.favorite,
                    lastUpdate = it.lastUpdate,
                    nextUpdate = it.nextUpdate,
                    calculateInterval = it.fetchInterval.toLong(),
                    initialized = it.initialized,
                    viewerFlags = it.viewerFlags,
                    chapterFlags = it.chapterFlags,
                    coverLastModified = it.coverLastModified,
                    dateAdded = it.dateAdded,
                    updateStrategy = it.updateStrategy,
                    version = it.version,
                    memo = it.memo,
                    // SY -->
                    updateTitle = it.ogTitle.isNotBlank(),
                    updateCover = !it.ogThumbnailUrl.isNullOrBlank(),
                    // SY <--
                    updateDetails = it.initialized,
                )
                    .awaitOne(MangaMapper::mapManga)
            }
        }
    }

    private suspend fun partialUpdate(mangaUpdates: List<MangaUpdate>) {
        database.transaction {
            mangaUpdates.forEach { value ->
                database.mangasQueries.update(
                    source = value.source,
                    url = value.url,
                    artist = value.artist,
                    author = value.author,
                    description = value.description,
                    genre = value.genre,
                    title = value.title,
                    status = value.status,
                    thumbnailUrl = value.thumbnailUrl,
                    favorite = value.favorite,
                    lastUpdate = value.lastUpdate,
                    nextUpdate = value.nextUpdate,
                    calculateInterval = value.fetchInterval?.toLong(),
                    initialized = value.initialized,
                    viewer = value.viewerFlags,
                    chapterFlags = value.chapterFlags,
                    coverLastModified = value.coverLastModified,
                    dateAdded = value.dateAdded,
                    mangaId = value.id,
                    updateStrategy = value.updateStrategy,
                    version = value.version,
                    isSyncing = 0,
                    notes = value.notes,
                    memo = value.memo,
                )
            }
        }
    }

    // SY -->
    override suspend fun deleteManga(mangaId: Long) {
        database.mangasQueries.deleteById(mangaId)
    }
    // SY <--
}
