package tachiyomi.domain.chapter.model

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

/**
 * A chapter row. [copyFrom] refreshes the source-reported fields from a newer fetch.
 *
 * @property id Row id; -1 until inserted.
 * @property mangaId Id of the manga the chapter belongs to.
 * @property read Whether the chapter has been read.
 * @property bookmark Whether the chapter is bookmarked.
 * @property lastPageRead Index of the last page read.
 * @property dateFetch Epoch millis the chapter was first fetched.
 * @property sourceOrder Position of the chapter in the source's listing.
 * @property url Path of the chapter on its source.
 * @property name Chapter title as reported by the source.
 * @property dateUpload Epoch millis of the upload date the source reported; -1 when unknown.
 * @property chapterNumber Parsed chapter number; negative when unrecognised.
 * @property scanlator Scanlation group, or null when unknown.
 * @property lastModifiedAt Epoch seconds of the last row change, for sync.
 * @property version Sync version counter.
 * @property memo Structured per-chapter data extensions may keep.
 */
public data class Chapter(
    val id: Long,
    val mangaId: Long,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Long,
    val dateFetch: Long,
    val sourceOrder: Long,
    val url: String,
    val name: String,
    val dateUpload: Long,
    val chapterNumber: Double,
    val scanlator: String?,
    val lastModifiedAt: Long,
    val version: Long,
    val memo: JsonObject,
) {
    /** Whether [chapterNumber] is a real number rather than the unknown sentinel. */
    val isRecognizedNumber: Boolean
        get() = chapterNumber >= 0f

    /** Factory of blank rows. */
    public companion object {
        /** A blank chapter with no id, no manga and an unrecognised number. */
        public fun create(): Chapter = Chapter(
            id = -1,
            mangaId = -1,
            read = false,
            bookmark = false,
            lastPageRead = 0,
            dateFetch = 0,
            sourceOrder = 0,
            url = "",
            name = "",
            dateUpload = -1,
            chapterNumber = -1.0,
            scanlator = null,
            lastModifiedAt = 0,
            version = 1,
            memo = JsonObject.EMPTY,
        )
    }
}

/** This chapter with the source-reported fields of [other]: name, url, upload date, number and scanlator. */
public fun Chapter.copyFrom(other: Chapter): Chapter = copy(
    name = other.name,
    url = other.url,
    dateUpload = other.dateUpload,
    chapterNumber = other.chapterNumber,
    scanlator = other.scanlator?.ifBlank { null },
)
