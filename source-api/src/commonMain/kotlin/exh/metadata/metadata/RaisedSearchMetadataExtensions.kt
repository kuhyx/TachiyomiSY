package exh.metadata.metadata

import exh.metadata.metadata.base.RaisedTag

/**
 * A label/value pair for the details screen, or null when [item] is null.
 *
 * @param T the value type.
 * @param item the value to show, skipped when null.
 * @param toString how the value is rendered; defaults to its `toString`.
 * @param block the label for a non-null [item].
 */
public fun <T : Any> RaisedSearchMetadata.getItem(
    item: T?,
    toString: (T) -> String = Any::toString,
    block: (T) -> String,
): Pair<String, String>? {
    item ?: return null
    return block(item) to toString(item)
}

/** The tags whose namespace is [ns]. */
public fun List<RaisedTag>.ofNamespace(ns: String): List<RaisedTag> = filter { it.namespace == ns }
