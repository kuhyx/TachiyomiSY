package exh.eh

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY
import tachiyomi.domain.chapter.model.Chapter

// Chapter bookkeeping when a discarded gallery's chapters fold into the accepted root.
// Union of read/bookmark state; a chapter never opened inherits the furthest page read in the chain.
internal fun Chapter.mergedWith(other: Chapter, newLastPageRead: Long?): Chapter {
    var lastPageRead = lastPageRead.coerceAtLeast(other.lastPageRead)
    if (newLastPageRead != null && lastPageRead <= 0) {
        lastPageRead = newLastPageRead
    }
    return copy(read = read || other.read, lastPageRead = lastPageRead, bookmark = bookmark || other.bookmark)
}

// A copy of this chapter for [mangaId], unsaved (id -1) and unnumbered until [renumber].
internal fun Chapter.copyInto(mangaId: Long, newLastPageRead: Long?): Chapter = Chapter(
    id = -1,
    mangaId = mangaId,
    url = url,
    name = name,
    read = read,
    bookmark = bookmark,
    lastPageRead = if (newLastPageRead != null && lastPageRead <= 0) newLastPageRead else lastPageRead,
    dateFetch = dateFetch,
    dateUpload = dateUpload,
    chapterNumber = -1.0,
    scanlator = null,
    sourceOrder = -1,
    lastModifiedAt = 0,
    version = 0,
    memo = JsonObject.EMPTY,
)

// Names and numbers the chapters "v1..vN" by upload order; new ones are inserted, existing ones updated.
