package tachiyomi.data.manga

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.getLibraryQuery
import tachiyomi.data.subscribeToList
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaLibraryRepository

/** [MangaLibraryRepository] on the SQLDelight `mangas` table and the hand-written library query (SY). */
internal class MangaLibraryRepositoryImpl(
    private val database: Database,
) : MangaLibraryRepository {

    override suspend fun getFavorites(): List<Manga> {
        return database.mangasQueries
            .getFavorites()
            .awaitList(MangaMapper::mapManga)
    }

    override suspend fun getReadMangaNotInLibrary(): List<Manga> {
        return database.mangasQueries
            .getReadMangaNotInLibrary()
            .awaitList(MangaMapper::mapManga)
    }

    override suspend fun getLibraryManga(): List<LibraryManga> {
        return getLibraryQuery()
            .awaitList(MangaMapper::mapLibraryManga)
    }

    override fun getLibraryMangaAsFlow(): Flow<List<LibraryManga>> {
        // SY: the generated view query only drives change notifications; the rows come from the merged-aware query.
        return database.libraryViewQueries
            .library()
            .subscribeToList()
            .map { getLibraryManga() }
    }

    // SY -->
    override suspend fun getReadMangaNotInLibraryView(): List<LibraryManga> {
        return getLibraryQuery("M.favorite = 0 AND C.readCount != 0")
            .awaitList(MangaMapper::mapLibraryManga)
    }
    // SY <--
}
