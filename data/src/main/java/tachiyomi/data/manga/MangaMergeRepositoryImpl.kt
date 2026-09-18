package tachiyomi.data.manga

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.domain.manga.model.MergeMangaSettingsUpdate
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.manga.repository.MangaMergeReadRepository
import tachiyomi.domain.manga.repository.MangaMergeRepository

/** [MangaMergeRepository] on the SQLDelight `merged` table (SY); reads are delegated. */
public class MangaMergeRepositoryImpl(
    private val database: Database,
) : MangaMergeRepository,
    MangaMergeReadRepository by MangaMergeReadRepositoryImpl(database) {

    override suspend fun updateSettings(update: MergeMangaSettingsUpdate): Boolean = updateAllSettings(listOf(update))

    override suspend fun updateAllSettings(values: List<MergeMangaSettingsUpdate>): Boolean {
        return try {
            database.transaction {
                values.forEach { value ->
                    database.mergedQueries.updateSettingsById(
                        id = value.id,
                        getChapterUpdates = value.getChapterUpdates,
                        downloadChapters = value.downloadChapters,
                        infoManga = value.isInfoManga,
                        chapterPriority = value.chapterPriority?.toLong(),
                        chapterSortMode = value.chapterSortMode?.toLong(),
                    )
                }
            }
            true
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            false
        }
    }

    override suspend fun insert(reference: MergedMangaReference): Long? {
        return database.mergedQueries
            .insertReturningId(
                infoManga = reference.isInfoManga,
                getChapterUpdates = reference.getChapterUpdates,
                chapterSortMode = reference.chapterSortMode.toLong(),
                chapterPriority = reference.chapterPriority.toLong(),
                downloadChapters = reference.downloadChapters,
                mergeId = reference.mergeId!!,
                mergeUrl = reference.mergeUrl,
                mangaId = reference.mangaId,
                mangaUrl = reference.mangaUrl,
                mangaSource = reference.mangaSourceId,
            )
            .awaitAsOneOrNull()
    }

    override suspend fun insertAll(references: List<MergedMangaReference>) {
        database.transaction {
            references.forEach { reference ->
                database.mergedQueries.insert(
                    infoManga = reference.isInfoManga,
                    getChapterUpdates = reference.getChapterUpdates,
                    chapterSortMode = reference.chapterSortMode.toLong(),
                    chapterPriority = reference.chapterPriority.toLong(),
                    downloadChapters = reference.downloadChapters,
                    mergeId = reference.mergeId!!,
                    mergeUrl = reference.mergeUrl,
                    mangaId = reference.mangaId,
                    mangaUrl = reference.mangaUrl,
                    mangaSource = reference.mangaSourceId,
                )
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        database.mergedQueries.deleteById(id)
    }

    override suspend fun deleteByMergeId(mergeId: Long) {
        database.mergedQueries.deleteByMergeId(mergeId)
    }
}
