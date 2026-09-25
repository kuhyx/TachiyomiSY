package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

/** A database chapter with the fields the reader reads; everything else stays at its default. */
internal fun dbChapter(
    id: Long = 1L,
    url: String = "/chapter/$id",
    mangaId: Long = 10L,
    read: Boolean = false,
    lastPageRead: Int = 0,
): Chapter = ChapterImpl().also {
    it.id = id
    it.url = url
    it.name = "Chapter $id"
    it.mangaId = mangaId
    it.read = read
    it.lastPageRead = lastPageRead
}

/** A reader chapter around [dbChapter]. */
internal fun readerChapter(id: Long = 1L, read: Boolean = false, lastPageRead: Int = 0): ReaderChapter =
    ReaderChapter(dbChapter(id = id, read = read, lastPageRead = lastPageRead))

/** [count] pages attached to [chapter], which is moved to the loaded state holding them. */
internal fun loadedPages(chapter: ReaderChapter, count: Int): List<ReaderPage> {
    val pages = List(count) { ReaderPage(it, url = "/p/$it", imageUrl = "https://img/$it") }
    pages.forEach { it.chapter = chapter }
    chapter.state = ReaderChapter.State.Loaded(pages)
    return pages
}
