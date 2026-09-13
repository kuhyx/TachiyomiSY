@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.source.model

import kotlinx.serialization.json.JsonObject
import java.io.Serializable

/** A manga as a source describes it; extensions fill these fields, the app reads them. */
public interface SManga : Serializable {

    /** URL relative to the source's base URL. */
    public var url: String

    /** Display title. */
    public var title: String

    /** Cover image URL. */
    public var thumbnail_url: String?

    /** Artist credit, comma separated. */
    public var artist: String?

    /** Author credit, comma separated. */
    public var author: String?

    /** One of the status constants in the companion. */
    public var status: Int

    /** Long description. */
    public var description: String?

    /** Genres, comma separated. */
    public var genre: String?

    /** How the app should look for new chapters. */
    public var update_strategy: UpdateStrategy

    /** True once details have been fetched at least once. */
    public var initialized: Boolean

    /**
     * Extra metadata associated with the manga.
     *
     * The JSON object is not visible to users and intended for internal or source-specific
     * purposes. Apps may define their own namespaced keys (e.g., `"mihon.*"`) for sources to populate.
     *
     * This allows apps to attach and ask for custom information without affecting the visible
     * manga data.
     *
     * @since tachiyomix 1.6
     */
    public var memo: JsonObject

    // SY -->

    /** [title] before any user edit. */
    public val originalTitle: String

    /** [author] before any user edit. */
    public val originalAuthor: String?

    /** [artist] before any user edit. */
    public val originalArtist: String?

    /** [thumbnail_url] before any user edit. */
    public val originalThumbnailUrl: String?

    /** [description] before any user edit. */
    public val originalDescription: String?

    /** [genre] before any user edit. */
    public val originalGenre: String?

    /** [status] before any user edit. */
    public val originalStatus: Int
    // SY <--

    /** [genre] split on commas, trimmed and deduplicated; null when blank. */
    public fun getGenres(): List<String>? {
        if (genre.isNullOrBlank()) return null
        return genre?.split(", ")?.map { it.trim() }?.filterNot { it.isBlank() }?.distinct()
    }

    /** A new instance with the same source-provided fields. */
    public fun copy(): SManga = create().also {
        it.url = url
        // SY -->
        it.title = originalTitle
        it.artist = originalArtist
        it.author = originalAuthor
        it.thumbnail_url = originalThumbnailUrl
        it.description = originalDescription
        it.genre = originalGenre
        it.status = originalStatus
        // SY <--
        it.update_strategy = update_strategy
        it.initialized = initialized
        it.memo = memo
    }

    /** Status constants and factories. */
    public companion object {
        /** Status not known. */
        public const val UNKNOWN: Int = 0

        /** Still being published. */
        public const val ONGOING: Int = 1

        /** Finished. */
        public const val COMPLETED: Int = 2

        /** Licensed, no longer served by the source. */
        public const val LICENSED: Int = 3

        /** Publication finished, translation may continue. */
        public const val PUBLISHING_FINISHED: Int = 4

        /** Cancelled before completion. */
        public const val CANCELLED: Int = 5

        /** Paused. */
        public const val ON_HIATUS: Int = 6

        /** An empty manga. */
        public fun create(): SManga = SMangaImpl()

        // SY -->

        /** A manga with the given fields set. */
        public operator fun invoke(
            url: String,
            title: String,
            artist: String? = null,
            author: String? = null,
            description: String? = null,
            genre: String? = null,
            status: Int = 0,
            thumbnail_url: String? = null,
            initialized: Boolean = false,
        ): SManga {
            return create().also {
                it.url = url
                it.title = title
                it.artist = artist
                it.author = author
                it.description = description
                it.genre = genre
                it.status = status
                it.thumbnail_url = thumbnail_url
                it.initialized = initialized
            }
        }
        // SY <--
    }
}

// SY -->

/** A copy with the given fields replaced; unspecified fields keep their original values. */
public fun SManga.copy(
    url: String = this.url,
    title: String = this.originalTitle,
    artist: String? = this.originalArtist,
    author: String? = this.originalAuthor,
    description: String? = this.originalDescription,
    genre: String? = this.originalGenre,
    status: Int = this.status,
    thumbnail_url: String? = this.originalThumbnailUrl,
    initialized: Boolean = this.initialized,
): SManga = SManga.create().also {
    it.url = url
    it.title = title
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = genre
    it.status = status
    it.thumbnail_url = thumbnail_url
    it.initialized = initialized
}
// SY <--
