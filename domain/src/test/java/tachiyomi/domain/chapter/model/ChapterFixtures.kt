package tachiyomi.domain.chapter.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A memo with one entry, distinguishable from the empty default. */
internal val sampleMemo: JsonObject = JsonObject(mapOf("key" to JsonPrimitive("value")))

/** A chapter row with every field set to a distinctive non-default value. */
internal fun fullChapter(): Chapter = Chapter(
    id = 1L,
    mangaId = 2L,
    read = true,
    bookmark = true,
    lastPageRead = 3L,
    dateFetch = 4L,
    sourceOrder = 5L,
    url = "/c1",
    name = "Chapter 1",
    dateUpload = 6L,
    chapterNumber = 1.0,
    scanlator = "Group",
    lastModifiedAt = 7L,
    version = 8L,
    memo = sampleMemo,
)

/** A chapter of [mangaId] numbered [number], with [id] as the row id. */
internal fun chapterOf(id: Long, mangaId: Long, number: Double): Chapter =
    Chapter.create().copy(id = id, mangaId = mangaId, chapterNumber = number)
