package tachiyomi.data.chapter

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.flow.Flow
import tachiyomi.core.common.util.lang.toLong
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.chapter.ChapterMapper.mapChapter
import tachiyomi.data.subscribeToList
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterMergedRepository

/** [ChapterMergedRepository] on the SQLDelight `chapters` and `merged` tables (SY). */
internal class ChapterMergedRepositoryImpl(
    private val database: Database,
) : ChapterMergedRepository {

    override suspend fun getMergedChapterByMangaId(mangaId: Long, applyScanlatorFilter: Boolean): List<Chapter> {
        return database.chaptersQueries
            .getMergedChaptersByMangaId(mangaId, applyScanlatorFilter.toLong())
            .awaitList(::mapChapter)
    }

    override suspend fun getMergedChapterByMangaIdFlow(
        mangaId: Long,
        applyScanlatorFilter: Boolean,
    ): Flow<List<Chapter>> {
        return database.chaptersQueries
            .getMergedChaptersByMangaId(mangaId, applyScanlatorFilter.toLong())
            .subscribeToList(::mapChapter)
    }

    override suspend fun getScanlatorsByMergeId(mangaId: Long): List<String> {
        return database.chaptersQueries
            .getScanlatorsByMergeId(mangaId) { it.orEmpty() }
            .awaitAsList()
    }

    override fun getScanlatorsByMergeIdAsFlow(mangaId: Long): Flow<List<String>> {
        return database.chaptersQueries
            .getScanlatorsByMergeId(mangaId) { it.orEmpty() }
            .subscribeToList()
    }
}
