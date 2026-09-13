package exh.metadata.sql.models

import kotlinx.serialization.Serializable

/**
 * The metadata row of a manga: the source-specific fields as JSON plus one indexed value.
 *
 * @property mangaId the manga this row belongs to.
 * @property uploader uploader name, if any.
 * @property extra the source-specific fields, JSON encoded.
 * @property indexedExtra one searchable value, such as a gallery id.
 * @property extraVersion schema version of [extra].
 */
@Serializable
public data class SearchMetadata(
    val mangaId: Long,

    val uploader: String?,

    val extra: String,

    val indexedExtra: String?,

    val extraVersion: Int,
)
