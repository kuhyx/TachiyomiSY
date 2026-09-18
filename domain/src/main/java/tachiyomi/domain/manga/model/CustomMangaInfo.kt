package tachiyomi.domain.manga.model

/**
 * The user's edits to a favourite's details; a null field falls back to what the source reported.
 *
 * @property id Row id of the manga the edits belong to.
 * @property title Edited title.
 * @property author Edited author.
 * @property artist Edited artist.
 * @property thumbnailUrl Edited cover url.
 * @property description Edited description.
 * @property genre Edited genres.
 * @property status Edited publishing status.
 */
public data class CustomMangaInfo(
    val id: Long,
    val title: String?,
    val author: String? = null,
    val artist: String? = null,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val genre: List<String>? = null,
    val status: Long? = null,
)
