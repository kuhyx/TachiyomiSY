package tachiyomi.domain.manga.model

/**
 * One entry of a merged manga: which source manga is merged in and how its chapters are treated.
 *
 * @property id Row id of the reference.
 * @property isInfoManga Whether this entry supplies the merged manga's details.
 * @property getChapterUpdates Whether library updates fetch this entry's chapters.
 * @property chapterSortMode Chapter dedupe mode, one of the `CHAPTER_SORT_*` constants; set on the main entry.
 * @property chapterPriority Rank used when deduplicating chapters by priority; lower wins.
 * @property downloadChapters Whether new chapters of this entry are downloaded automatically.
 * @property mergeId Row id of the merged manga this reference belongs to.
 * @property mergeUrl Url of the merged manga this reference belongs to.
 * @property mangaId Row id of the source manga merged in, once it exists locally.
 * @property mangaUrl Url of the source manga merged in.
 * @property mangaSourceId Id of the source the merged-in manga comes from.
 */
public data class MergedMangaReference(
    // Tag identifier, unique
    val id: Long,

    // The manga where it grabs the updated manga info
    val isInfoManga: Boolean,

    // If false the manga will not grab chapter updates
    val getChapterUpdates: Boolean,

    // The mode in which the chapters are handeled, only set in the main merge reference
    val chapterSortMode: Int,

    // chapter priority the deduplication uses
    val chapterPriority: Int,

    // Set if you want it to download new chapters
    val downloadChapters: Boolean,

    // merged manga this reference is attached to
    val mergeId: Long?,

    // merged manga url this reference is attached to
    val mergeUrl: String,

    // manga id included in the merge this reference is attached to
    val mangaId: Long?,

    // manga url included in the merge this reference is attached to
    val mangaUrl: String,

    // source of the manga that is merged into this merge
    val mangaSourceId: Long,
) {
    /** The chapter dedupe modes a merged manga can use. */
    public companion object {
        /** Dedupe mode: unset; behaves like [CHAPTER_SORT_NO_DEDUPE]. */
        public const val CHAPTER_SORT_NONE: Int = 0

        /** Dedupe mode: show every entry's chapters. */
        public const val CHAPTER_SORT_NO_DEDUPE: Int = 1

        /** Dedupe mode: keep the chapter from the entry with the lowest [chapterPriority]. */
        public const val CHAPTER_SORT_PRIORITY: Int = 2

        /** Dedupe mode: show only the entry with the most chapters. */
        public const val CHAPTER_SORT_MOST_CHAPTERS: Int = 3

        /** Dedupe mode: show only the entry with the highest chapter number. */
        public const val CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER: Int = 4
    }
}
