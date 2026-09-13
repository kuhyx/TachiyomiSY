package exh.metadata.sql.models

import kotlinx.serialization.Serializable

/**
 * A tag row of the metadata search index.
 *
 * @property id row id, null before insertion.
 * @property mangaId the manga the tag belongs to.
 * @property namespace tag namespace, if any.
 * @property name tag name.
 * @property type source-specific tag type.
 */
@Serializable
public data class SearchTag(
    val id: Long?,

    val mangaId: Long,

    val namespace: String?,

    val name: String,

    val type: Int,
)
