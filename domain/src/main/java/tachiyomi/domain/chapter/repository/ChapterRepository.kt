package tachiyomi.domain.chapter.repository

/**
 * Persistence of chapters. The members live in the three facets so each stays
 * under the function-count cap; this is the type callers depend on.
 */
public interface ChapterRepository : ChapterWriteRepository, ChapterQueryRepository, ChapterMergedRepository
