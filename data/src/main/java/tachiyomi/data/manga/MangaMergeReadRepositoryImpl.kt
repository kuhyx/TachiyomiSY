package tachiyomi.data.manga

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.subscribeToList
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.manga.repository.MangaMergeReadRepository

/** [MangaMergeReadRepository] on the SQLDelight `merged` table (SY). */
internal class MangaMergeReadRepositoryImpl(
    private val database: Database,
) : MangaMergeReadRepository {

    override suspend fun getMergedManga(): List<Manga> {
        return database.mergedQueries
            .selectAllMergedMangas()
            .awaitList(MangaMapper::mapManga)
    }

    override suspend fun subscribeMergedManga(): Flow<List<Manga>> {
        return database.mergedQueries
            .selectAllMergedMangas()
            .subscribeToList(MangaMapper::mapManga)
    }

    override suspend fun getMergedMangaById(id: Long): List<Manga> {
        return database.mergedQueries
            .selectMergedMangasById(id)
            .awaitList(MangaMapper::mapManga)
    }

    override suspend fun subscribeMergedMangaById(id: Long): Flow<List<Manga>> {
        return database.mergedQueries
            .selectMergedMangasById(id)
            .subscribeToList(MangaMapper::mapManga)
    }

    override suspend fun getReferencesById(id: Long): List<MergedMangaReference> {
        return database.mergedQueries
            .selectByMergeId(id)
            .awaitList(MergedMangaMapper::map)
    }

    override suspend fun subscribeReferencesById(id: Long): Flow<List<MergedMangaReference>> {
        return database.mergedQueries
            .selectByMergeId(id)
            .subscribeToList(MergedMangaMapper::map)
    }

    override suspend fun getMergeMangaForDownloading(mergeId: Long): List<Manga> {
        return database.mergedQueries
            .selectMergedMangasForDownloadingById(mergeId)
            .awaitList(MangaMapper::mapManga)
    }
}
