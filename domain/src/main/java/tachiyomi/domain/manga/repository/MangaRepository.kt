package tachiyomi.domain.manga.repository

/**
 * Persistence of manga. The members live in the three facets so each stays
 * under the function-count cap; this is the type callers depend on.
 */
public interface MangaRepository : MangaQueryRepository, MangaLibraryRepository, MangaWriteRepository
