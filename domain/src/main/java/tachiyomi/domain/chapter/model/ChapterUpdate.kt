package tachiyomi.domain.chapter.model

import kotlinx.serialization.json.JsonObject

/**
 * A partial update of one [Chapter] row; a null field leaves that column unchanged.
 *
 * @property id Row id of the chapter to update.
 * @property mangaId Id of the manga the chapter belongs to.
 * @property read Whether the chapter has been read.
 * @property bookmark Whether the chapter is bookmarked.
 * @property lastPageRead Index of the last page read.
 * @property dateFetch Epoch millis the chapter was first fetched.
 * @property sourceOrder Position of the chapter in the source's listing.
 * @property url Path of the chapter on its source.
 * @property name Chapter title as reported by the source.
 * @property dateUpload Epoch millis of the upload date the source reported.
 * @property chapterNumber Parsed chapter number; negative when unrecognised.
 * @property scanlator Scanlation group, or null when unknown.
 * @property version Sync version counter.
 * @property memo Structured per-chapter data extensions may keep.
 */
public data class ChapterUpdate(
    val id: Long,
    val mangaId: Long? = null,
    val read: Boolean? = null,
    val bookmark: Boolean? = null,
    val lastPageRead: Long? = null,
    val dateFetch: Long? = null,
    val sourceOrder: Long? = null,
    val url: String? = null,
    val name: String? = null,
    val dateUpload: Long? = null,
    val chapterNumber: Double? = null,
    val scanlator: String? = null,
    val version: Long? = null,
    val memo: JsonObject? = null,
)

/** An update that rewrites every column of this chapter with its current values. */
public fun Chapter.toChapterUpdate(): ChapterUpdate {
    return ChapterUpdate(
        id,
        mangaId,
        read,
        bookmark,
        lastPageRead,
        dateFetch,
        sourceOrder,
        url,
        name,
        dateUpload,
        chapterNumber,
        scanlator,
        version,
        memo,
    )
}
