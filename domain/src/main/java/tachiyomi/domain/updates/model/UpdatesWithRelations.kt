package tachiyomi.domain.updates.model

import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.MangaCover
import uy.kohesive.injekt.injectLazy

/**
 * An updates feed row: a recently uploaded chapter joined with its manga.
 *
 * @property mangaId Id of the manga the chapter belongs to.
 * @property ogMangaTitle Manga title as reported by the source; [mangaTitle] applies the user's edit.
 * @property chapterId Id of the chapter.
 * @property chapterName Name of the chapter as reported by the source.
 * @property scanlator Scanlation group of the chapter; null when the source gives none.
 * @property chapterUrl Path of the chapter on its source.
 * @property read Whether the chapter is marked read.
 * @property bookmark Whether the chapter is bookmarked.
 * @property lastPageRead Index of the last page read; 0 when unstarted.
 * @property sourceId Id of the source the manga comes from.
 * @property dateFetch Epoch millis the chapter was first fetched.
 * @property coverData What the cover loader needs to show the manga's cover.
 */
public data class UpdatesWithRelations(
    val mangaId: Long,
    // SY -->
    val ogMangaTitle: String,
    // SY <--
    val chapterId: Long,
    val chapterName: String,
    val scanlator: String?,
    val chapterUrl: String,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Long,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: MangaCover,
) {
    // SY -->
    /** The manga title to show: the user's custom title when set, else [ogMangaTitle]. */
    val mangaTitle: String = getCustomMangaInfo.get(mangaId)?.title ?: ogMangaTitle

    /** The custom-title lookup shared by every row. */
    public companion object {
        internal val getCustomMangaInfo: GetCustomMangaInfo by injectLazy()
    }
    // SY <--
}
