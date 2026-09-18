package tachiyomi.domain.manga.model

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.serialization.json.JsonObject

/**
 * A partial update of one [Manga] row; a null field leaves that column unchanged.
 *
 * @property id Row id of the manga to update.
 * @property source Id of the source this manga comes from.
 * @property favorite Whether it is in the library.
 * @property lastUpdate Epoch millis of the last chapter fetch.
 * @property nextUpdate Epoch millis when the next chapter is expected.
 * @property fetchInterval Days between expected chapters; negative when set by the user.
 * @property dateAdded Epoch millis the manga was added to the library.
 * @property viewerFlags Reader mode and orientation bits.
 * @property chapterFlags Chapter sort, display and filter bits.
 * @property coverLastModified Epoch millis of the last cover change, for cache keys.
 * @property url Path of the manga on its source.
 * @property title Title as reported by the source.
 * @property artist Artist as reported by the source.
 * @property author Author as reported by the source.
 * @property description Description as reported by the source.
 * @property genre Genres as reported by the source.
 * @property status Publishing status as reported by the source.
 * @property thumbnailUrl Cover url as reported by the source.
 * @property updateStrategy Whether library updates fetch new chapters.
 * @property initialized Whether details have been fetched at least once.
 * @property version Sync version counter.
 * @property notes The user's free-text notes.
 * @property memo Structured per-manga data extensions may keep.
 * @property filteredScanlators Scanlator names whose chapters are hidden.
 */
public data class MangaUpdate(
    val id: Long,
    val source: Long? = null,
    val favorite: Boolean? = null,
    val lastUpdate: Long? = null,
    val nextUpdate: Long? = null,
    val fetchInterval: Int? = null,
    val dateAdded: Long? = null,
    val viewerFlags: Long? = null,
    val chapterFlags: Long? = null,
    val coverLastModified: Long? = null,
    val url: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val author: String? = null,
    val description: String? = null,
    val genre: List<String>? = null,
    val status: Long? = null,
    val thumbnailUrl: String? = null,
    val updateStrategy: UpdateStrategy? = null,
    val initialized: Boolean? = null,
    val version: Long? = null,
    val notes: String? = null,
    val memo: JsonObject? = null,
    // SY -->
    val filteredScanlators: List<String>? = null,
    // SY <--
)

/** A [MangaUpdate] carrying every column of this manga, with the source-reported `og*` values. */
public fun Manga.toMangaUpdate(): MangaUpdate {
    return MangaUpdate(
        id = id,
        source = source,
        favorite = favorite,
        lastUpdate = lastUpdate,
        nextUpdate = nextUpdate,
        fetchInterval = fetchInterval,
        dateAdded = dateAdded,
        viewerFlags = viewerFlags,
        chapterFlags = chapterFlags,
        coverLastModified = coverLastModified,
        url = url,
        // SY -->
        title = ogTitle,
        artist = ogArtist,
        author = ogAuthor,
        thumbnailUrl = ogThumbnailUrl,
        description = ogDescription,
        genre = ogGenre,
        status = ogStatus,
        // SY <--
        updateStrategy = updateStrategy,
        initialized = initialized,
        version = version,
        notes = notes,
        memo = memo,
    )
}
