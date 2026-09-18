package tachiyomi.domain.manga.model

/**
 * A manga paired with how many chapters it has, as returned by duplicate-library lookups.
 *
 * @property manga The manga row.
 * @property chapterCount Number of chapters stored for [manga].
 */
public data class MangaWithChapterCount(
    val manga: Manga,
    val chapterCount: Long,
)
