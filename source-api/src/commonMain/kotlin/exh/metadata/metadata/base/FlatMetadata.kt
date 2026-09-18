package exh.metadata.metadata.base

import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Metadata in its database shape: one row plus its tags and titles.
 *
 * @property metadata the metadata row.
 * @property tags the tag rows.
 * @property titles the title rows.
 */
@Serializable
public data class FlatMetadata(
    val metadata: SearchMetadata,
    val tags: List<SearchTag>,
    val titles: List<SearchTitle>,
)

/** Inflates the stored metadata into an instance of [clazz], filling the base fields from this row. */
@OptIn(InternalSerializationApi::class)
public fun <T : RaisedSearchMetadata> FlatMetadata.raise(clazz: KClass<T>): T {
    val raised = RaisedSearchMetadata.raiseFlattenJson.decodeFromString(clazz.serializer(), metadata.extra)
    raised.fillBaseFields(this)
    return raised
}
