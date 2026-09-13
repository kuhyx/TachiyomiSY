package eu.kanade.tachiyomi.source.model

import androidx.compose.runtime.Stable

/**
 * A source's filters as one list; equality is deliberately identity-like so Compose always recomposes.
 *
 * @property list the filters.
 */
@Stable
public data class FilterList(val list: List<Filter<*>>) : List<Filter<*>> by list {

    public constructor(vararg fs: Filter<*>) : this(if (fs.isNotEmpty()) fs.asList() else emptyList())

    override fun equals(other: Any?): Boolean = false

    override fun hashCode(): Int = list.hashCode()
}
