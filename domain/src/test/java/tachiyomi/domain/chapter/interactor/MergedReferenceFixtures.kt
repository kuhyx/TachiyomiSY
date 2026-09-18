package tachiyomi.domain.chapter.interactor

import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.manga.model.MergedMangaReference

/** A reference of merge 99 pointing at [mangaId] on [mangaSourceId], with the given dedupe settings. */
internal fun mergedReference(
    mangaId: Long?,
    mangaSourceId: Long,
    chapterSortMode: Int = MergedMangaReference.CHAPTER_SORT_NONE,
    chapterPriority: Int = 0,
): MergedMangaReference = MergedMangaReference(
    id = mangaId ?: -1L,
    isInfoManga = false,
    getChapterUpdates = true,
    chapterSortMode = chapterSortMode,
    chapterPriority = chapterPriority,
    downloadChapters = false,
    mergeId = 99L,
    mergeUrl = "/merge",
    mangaId = mangaId,
    mangaUrl = "/manga",
    mangaSourceId = mangaSourceId,
)

/** The merge's own reference, which carries the dedupe [chapterSortMode]. */
internal fun mergeReference(chapterSortMode: Int): MergedMangaReference =
    mergedReference(mangaId = null, mangaSourceId = MERGED_SOURCE_ID, chapterSortMode = chapterSortMode)
