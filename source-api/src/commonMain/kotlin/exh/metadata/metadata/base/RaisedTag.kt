package exh.metadata.metadata.base

import kotlinx.serialization.Serializable

/**
 * A tag as scraped from a site, before it is stored.
 *
 * @property namespace tag namespace, if any.
 * @property name tag name.
 * @property type source-specific tag type.
 */
@Serializable
public data class RaisedTag(
    val namespace: String?,
    val name: String,
    val type: Int,
)
