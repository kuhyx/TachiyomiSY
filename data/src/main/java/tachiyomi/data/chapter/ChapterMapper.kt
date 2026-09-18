package tachiyomi.data.chapter

import tachiyomi.data.Chapters
import tachiyomi.domain.chapter.model.Chapter

/** Domain models from the generated `chapters` rows. */
public object ChapterMapper {
    /** The [Chapter] of a `chapters` row. */
    public fun mapChapter(row: Chapters): Chapter = Chapter(
        id = row._id,
        mangaId = row.manga_id,
        read = row.read,
        bookmark = row.bookmark,
        lastPageRead = row.last_page_read,
        dateFetch = row.date_fetch,
        sourceOrder = row.source_order,
        url = row.url,
        name = row.name,
        dateUpload = row.date_upload,
        chapterNumber = row.chapter_number,
        scanlator = row.scanlator,
        lastModifiedAt = row.last_modified_at,
        version = row.version,
        memo = row.memo,
    )
}
