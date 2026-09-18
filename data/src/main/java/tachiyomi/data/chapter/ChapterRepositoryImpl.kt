package tachiyomi.data.chapter

import tachiyomi.data.Database
import tachiyomi.domain.chapter.repository.ChapterMergedRepository
import tachiyomi.domain.chapter.repository.ChapterQueryRepository
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.chapter.repository.ChapterWriteRepository

/** [ChapterRepository] on SQLDelight: the three facets composed, one per file. */
public class ChapterRepositoryImpl(
    database: Database,
) : ChapterRepository,
    ChapterWriteRepository by ChapterWriteRepositoryImpl(database),
    ChapterQueryRepository by ChapterQueryRepositoryImpl(database),
    ChapterMergedRepository by ChapterMergedRepositoryImpl(database)
