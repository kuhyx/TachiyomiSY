package tachiyomi.domain.history.model

import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.MangaCover
import uy.kohesive.injekt.injectLazy
import java.util.Date

/**
 * A history screen row: a history entry joined with its chapter and manga.
 *
 * @property id Row id of the history entry.
 * @property chapterId Id of the chapter that was read.
 * @property mangaId Id of the manga the chapter belongs to.
 * @property ogTitle Manga title as reported by the source; [title] applies the user's edit.
 * @property chapterNumber Number of the chapter that was read.
 * @property readAt When the chapter was last read; null when never.
 * @property readDuration Total milliseconds spent reading the chapter.
 * @property coverData What the cover loader needs to show the manga's cover.
 */
public data class HistoryWithRelations(
    val id: Long,
    val chapterId: Long,
    val mangaId: Long,
    // SY -->
    val ogTitle: String,
    // SY <--
    val chapterNumber: Double,
    val readAt: Date?,
    val readDuration: Long,
    val coverData: MangaCover,
) {
    // SY -->
    /** The manga title to show: the user's custom title when set, else [ogTitle]. */
    val title: String = customMangaManager.get(mangaId)?.title ?: ogTitle

    /** The custom-title lookup shared by every row. */
    public companion object {
        private val customMangaManager: GetCustomMangaInfo by injectLazy()
    }
    // SY <--
}
