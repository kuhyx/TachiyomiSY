package exh.metadata.metadata.base

import kotlinx.serialization.Serializable

/**
 * A title as scraped from a site, before it is stored.
 *
 * @property title the title text.
 * @property type source-specific title type.
 */
@Serializable
public data class RaisedTitle(
    val title: String,
    val type: Int = 0,
)
