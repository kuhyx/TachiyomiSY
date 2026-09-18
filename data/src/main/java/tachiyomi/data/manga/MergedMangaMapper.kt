package tachiyomi.data.manga

import tachiyomi.data.Merged
import tachiyomi.domain.manga.model.MergedMangaReference

/** Domain models from the generated `merged` rows (SY). */
public object MergedMangaMapper {
    /** The [MergedMangaReference] of a `merged` row. */
    public fun map(row: Merged): MergedMangaReference = MergedMangaReference(
        id = row._id,
        isInfoManga = row.info_manga,
        getChapterUpdates = row.get_chapter_updates,
        chapterSortMode = row.chapter_sort_mode.toInt(),
        chapterPriority = row.chapter_priority.toInt(),
        downloadChapters = row.download_chapters,
        mergeId = row.merge_id,
        mergeUrl = row.merge_url,
        mangaId = row.manga_id,
        mangaUrl = row.manga_url,
        mangaSourceId = row.manga_source,
    )
}
