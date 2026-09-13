package tachiyomi.core.metadata.tachiyomi

import kotlinx.serialization.Serializable

/**
 * The `details.json` sidecar of a local-source manga; every field is optional.
 *
 * @property title display title.
 * @property author author credit.
 * @property artist artist credit.
 * @property description long description.
 * @property genre genre tags.
 * @property status an [eu.kanade.tachiyomi.source.model.SManga] status code.
 */
@Serializable
public data class MangaDetails(
    public val title: String? = null,
    public val author: String? = null,
    public val artist: String? = null,
    public val description: String? = null,
    public val genre: List<String>? = null,
    public val status: Int? = null,
)
