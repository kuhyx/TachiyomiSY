@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.source.model

import kotlinx.serialization.json.JsonObject
import java.io.Serializable

public interface SManga : Serializable {

    public var url: String

    public var title: String

    public var thumbnail_url: String?

    public var artist: String?

    public var author: String?

    public var status: Int

    public var description: String?

    public var genre: String?

    public var update_strategy: UpdateStrategy

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

    public fun getGenres(): List<String>? {
        if (genre.isNullOrBlank()) return null
        return genre?.split(", ")?.map { it.trim() }?.filterNot { it.isBlank() }?.distinct()
    }

    // SY -->
    public val originalTitle: String
    public val originalAuthor: String?
    public val originalArtist: String?
    public val originalThumbnailUrl: String?
    public val originalDescription: String?
    public val originalGenre: String?
    public val originalStatus: Int
    // SY <--

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

    public companion object {
        public const val UNKNOWN: Int = 0
        public const val ONGOING: Int = 1
        public const val COMPLETED: Int = 2
        public const val LICENSED: Int = 3
        public const val PUBLISHING_FINISHED: Int = 4
        public const val CANCELLED: Int = 5
        public const val ON_HIATUS: Int = 6

        public fun create(): SManga {
            return SMangaImpl()
        }

        // SY -->
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
