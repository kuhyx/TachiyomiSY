package tachiyomi.data.manga

import tachiyomi.data.Database
import tachiyomi.domain.manga.repository.MangaLibraryRepository
import tachiyomi.domain.manga.repository.MangaQueryRepository
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.manga.repository.MangaWriteRepository

/** [MangaRepository] on SQLDelight: the three facets composed, one per file. */
public class MangaRepositoryImpl(
    database: Database,
) : MangaRepository,
    MangaQueryRepository by MangaQueryRepositoryImpl(database),
    MangaLibraryRepository by MangaLibraryRepositoryImpl(database),
    MangaWriteRepository by MangaWriteRepositoryImpl(database)
