package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.download.model.Download
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

/** A chapter of manga 1 with [id], numbered [number] unless given. */
internal fun chapter(
    id: Long,
    number: Double = id.toDouble(),
    read: Boolean = false,
    bookmark: Boolean = false,
): Chapter = Chapter.create().copy(
    id = id,
    mangaId = 1L,
    chapterNumber = number,
    sourceOrder = id,
    read = read,
    bookmark = bookmark,
    name = "Chapter $id",
    url = "/c/$id",
)

/** A chapter row for [chapter]; not downloaded and unselected unless given. */
internal fun item(
    chapter: Chapter,
    state: Download.State = Download.State.NOT_DOWNLOADED,
    selected: Boolean = false,
): ChapterList.Item = ChapterList.Item(
    chapter = chapter,
    downloadState = state,
    downloadProgress = 0,
    selected = selected,
    sourceName = null,
    showScanlator = false,
)

/** Rows for chapters 1..[count]. */
internal fun items(count: Int): List<ChapterList.Item> = (1L..count).map { item(chapter(it)) }

/** Manga 1 of source 7 with the given chapter [flags]. */
internal fun manga(flags: Long = 0L, source: Long = 7L, favorite: Boolean = false): Manga = Manga.create().copy(
    id = 1L,
    source = source,
    title = "Needle",
    url = "/m/1",
    chapterFlags = flags,
    favorite = favorite,
    initialized = true,
)
