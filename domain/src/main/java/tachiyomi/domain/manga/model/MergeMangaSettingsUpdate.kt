package tachiyomi.domain.manga.model

/**
 * A partial update of one [MergedMangaReference]'s settings; a null field leaves that column unchanged.
 *
 * @property id Id of the reference row to update.
 * @property isInfoManga Whether this entry supplies the merged manga's details.
 * @property getChapterUpdates Whether library updates fetch this entry's chapters.
 * @property chapterPriority Rank used when deduplicating chapters by priority; lower wins.
 * @property downloadChapters Whether new chapters of this entry are downloaded automatically.
 * @property chapterSortMode Chapter dedupe mode, one of the `CHAPTER_SORT_*` constants.
 */
public data class MergeMangaSettingsUpdate(
    val id: Long,
    val isInfoManga: Boolean?,
    val getChapterUpdates: Boolean?,
    val chapterPriority: Int?,
    val downloadChapters: Boolean?,
    val chapterSortMode: Int?,
)
