package eu.kanade.tachiyomi.source.model

/**
 * A manga together with its chapter list, as returned by one combined source request.
 *
 * @property manga the updated manga.
 * @property chapters the updated chapter list.
 */
public data class SMangaUpdate(public val manga: SManga, public val chapters: List<SChapter>)
