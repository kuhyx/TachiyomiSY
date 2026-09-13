@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.source.model

import kotlinx.serialization.json.JsonObject
import java.io.Serializable

/** A chapter as a source describes it; extensions fill these fields, the app reads them. */

public interface SChapter : Serializable {

    /** URL relative to the source's base URL. */

    public var url: String

    /** Display name. */

    public var name: String

    /** Chapter number, -1 when unknown. */

    public var chapter_number: Float

    /** Scanlator credit. */

    public var scanlator: String?

    /** Upload time in epoch milliseconds, 0 when unknown. */

    public var date_upload: Long

    /**
     * Extra metadata associated with the chapter.
     *
     * The JSON object is not visible to users and intended for internal or source-specific
     * purposes. Apps may define their own namespaced keys (e.g., `"mihon.*"`) for sources to populate.
     *
     * This allows apps to attach and ask for custom information without affecting the visible
     * chapter data.
     *
     * @since tachiyomix 1.6
     */
    public var memo: JsonObject

    /** Copies every field of [other] into this chapter. */

    public fun copyFrom(other: SChapter) {
        name = other.name
        url = other.url
        date_upload = other.date_upload
        chapter_number = other.chapter_number
        scanlator = other.scanlator
        memo = other.memo
    }

    /** Factories. */

    public companion object {
        /** An empty chapter. */
        public fun create(): SChapter = SChapterImpl()

        // SY -->

        /** A chapter with the given fields set. */
        public operator fun invoke(
            name: String,
            url: String,
            dateUpload: Long = 0,
            chapterNumber: Float = -1F,
            scanlator: String? = null,
        ): SChapter {
            return create().apply {
                this.name = name
                this.url = url
                this.date_upload = dateUpload
                this.chapter_number = chapterNumber
                this.scanlator = scanlator
            }
        }
        // SY <--
    }
}
