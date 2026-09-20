@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.model.SChapter
import java.io.Serializable
import tachiyomi.domain.chapter.model.Chapter as DomainChapter

internal interface Chapter : SChapter, Serializable {

    var id: Long?

    var mangaId: Long?

    var read: Boolean

    var bookmark: Boolean

    var lastPageRead: Int

    var dateFetch: Long

    var sourceOrder: Int

    var lastModified: Long

    var version: Long
}

internal val Chapter.isRecognizedNumber: Boolean
    get() = chapter_number >= 0f

internal fun Chapter.toDomainChapter(): DomainChapter? {
    if (id == null || mangaId == null) return null
    return DomainChapter(
        id = id!!,
        mangaId = mangaId!!,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead.toLong(),
        dateFetch = dateFetch,
        sourceOrder = sourceOrder.toLong(),
        url = url,
        name = name,
        dateUpload = date_upload,
        chapterNumber = chapter_number.toDouble(),
        scanlator = scanlator,
        lastModifiedAt = lastModified,
        version = version,
        memo = memo,
    )
}
