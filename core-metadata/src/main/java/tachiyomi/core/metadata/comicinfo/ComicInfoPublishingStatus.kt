package tachiyomi.core.metadata.comicinfo

import eu.kanade.tachiyomi.source.model.SManga

/**
 * Maps the ComicInfo publishing-status text to [SManga]'s status codes.
 *
 * @property comicInfoValue the text written to and read from the document.
 * @property sMangaModelValue the matching [SManga] status constant.
 */
public enum class ComicInfoPublishingStatus(
    public val comicInfoValue: String,
    public val sMangaModelValue: Int,
) {
    /** Still being published. */
    ONGOING("Ongoing", SManga.ONGOING),

    /** Finished and fully released. */
    COMPLETED("Completed", SManga.COMPLETED),

    /** Licensed for official release. */
    LICENSED("Licensed", SManga.LICENSED),

    /** Publication finished, releases may still follow. */
    PUBLISHING_FINISHED("Publishing finished", SManga.PUBLISHING_FINISHED),

    /** Cancelled before completion. */
    CANCELLED("Cancelled", SManga.CANCELLED),

    /** Paused by the author or publisher. */
    ON_HIATUS("On hiatus", SManga.ON_HIATUS),

    /** No status known; the fallback in both directions. */
    UNKNOWN("Unknown", SManga.UNKNOWN),
    ;

    /** Lookups in both directions, unknown values map to [UNKNOWN]. */
    public companion object {
        /** The document text for an [SManga] status [value]. */
        public fun toComicInfoValue(value: Long): String =
            (entries.firstOrNull { it.sMangaModelValue == value.toInt() } ?: UNKNOWN).comicInfoValue

        /** The [SManga] status for a document text [value]; null or unknown text is [UNKNOWN]. */
        public fun toSMangaValue(value: String?): Int =
            (entries.firstOrNull { it.comicInfoValue == value } ?: UNKNOWN).sMangaModelValue
    }
}
