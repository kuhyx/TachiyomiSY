package tachiyomi.data.manga

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.awaitOne
import tachiyomi.data.awaitOneOrNull
import tachiyomi.data.subscribeToList
import tachiyomi.data.subscribeToOne
import tachiyomi.data.subscribeToOneOrNull
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.manga.repository.MangaQueryRepository
import java.time.LocalDate
import java.time.ZoneId

/** [MangaQueryRepository] on the SQLDelight `mangas` table. */
internal class MangaQueryRepositoryImpl(
    private val database: Database,
) : MangaQueryRepository {

    override suspend fun getMangaById(id: Long): Manga {
        return database.mangasQueries
            .getMangaById(id)
            .awaitOne(MangaMapper::mapManga)
    }

    override suspend fun getMangaByIdAsFlow(id: Long): Flow<Manga> {
        return database.mangasQueries
            .getMangaById(id)
            .subscribeToOne(MangaMapper::mapManga)
    }

    override suspend fun getMangaByUrlAndSourceId(url: String, sourceId: Long): Manga? {
        return database.mangasQueries
            .getMangaByUrlAndSource(url, sourceId)
            .awaitOneOrNull(MangaMapper::mapManga)
    }

    override fun getMangaByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Manga?> {
        return database.mangasQueries
            .getMangaByUrlAndSource(url, sourceId)
            .subscribeToOneOrNull(MangaMapper::mapManga)
    }

    override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Manga>> {
        return database.mangasQueries
            .getFavoriteBySourceId(sourceId)
            .subscribeToList(MangaMapper::mapManga)
    }

    override suspend fun getDuplicateLibraryManga(id: Long, title: String): List<MangaWithChapterCount> {
        return database.mangasQueries
            .getDuplicateLibraryManga(id, title)
            .awaitList(MangaMapper::mapMangaWithChapterCount)
    }

    override suspend fun getUpcomingManga(statuses: Set<Long>): Flow<List<Manga>> {
        val epochMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * MILLIS_PER_SECOND
        return database.mangasQueries
            .getUpcomingManga(epochMillis, statuses)
            .subscribeToList(MangaMapper::mapManga)
    }

    // SY -->
    override suspend fun getMangaBySourceId(sourceId: Long): List<Manga> {
        return database.mangasQueries
            .getBySource(sourceId)
            .awaitList(MangaMapper::mapManga)
    }

    override suspend fun getAll(): List<Manga> {
        return database.mangasQueries
            .getAll()
            .awaitList(MangaMapper::mapManga)
    }
    // SY <--

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
