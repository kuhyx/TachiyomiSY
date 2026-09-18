package eu.kanade.tachiyomi.source.model

import kotlinx.serialization.json.JsonObject

/**
 * An [SManga] whose [genre] getter answers once and then returns null, so the second null check
 * inside [SManga.getGenres] (the `genre?.split` safe call) takes its null branch.
 */
internal class VanishingGenreManga : SManga {
    private var genreReads = 0

    override var url: String = ""
    override var title: String = ""
    override var thumbnail_url: String? = null
    override var artist: String? = null
    override var author: String? = null
    override var status: Int = 0
    override var description: String? = null
    override var genre: String? = "Action"
        get() {
            val value = if (genreReads == 0) field else null
            genreReads += 1
            return value
        }
    override var update_strategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE
    override var initialized: Boolean = false
    override var memo: JsonObject = JsonObject(emptyMap())
    override val originalTitle: String
        get() = title
    override val originalAuthor: String?
        get() = author
    override val originalArtist: String?
        get() = artist
    override val originalThumbnailUrl: String?
        get() = thumbnail_url
    override val originalDescription: String?
        get() = description
    override val originalGenre: String?
        get() = genre
    override val originalStatus: Int
        get() = status

    /** How many times [genre] has been read. */
    fun genreReadCount(): Int = genreReads
}

/** A fully populated manga to copy from. */
internal fun populatedManga(): SManga = SManga(
    url = "/manga/1",
    title = "Title",
    artist = "Artist",
    author = "Author",
    description = "Description",
    genre = "Action, Drama",
    status = SManga.COMPLETED,
    thumbnailUrl = "https://example.invalid/cover.png",
    initialized = true,
)
