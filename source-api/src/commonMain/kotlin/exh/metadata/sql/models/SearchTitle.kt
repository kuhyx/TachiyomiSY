package exh.metadata.sql.models

import kotlinx.serialization.Serializable

/**
 * A title row of the metadata search index.
 *
 * @property id row id, null before insertion.
 * @property mangaId the manga the title belongs to.
 * @property title the title text.
 * @property type source-specific title type, such as main or alternative.
 */
@Serializable
public data class SearchTitle(
    val id: Long?,

    val mangaId: Long,

    val title: String,

    val type: Int,
)
