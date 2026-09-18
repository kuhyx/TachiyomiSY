package tachiyomi.domain.manga.model

import tachiyomi.core.common.preference.TriState

/**
 * Whether an item passes a tri-state [filter]: always when disabled, [predicate] when set to include,
 * its negation when set to exclude.
 */
public inline fun applyFilter(filter: TriState, predicate: () -> Boolean): Boolean = when (filter) {
    TriState.DISABLED -> true
    TriState.ENABLED_IS -> predicate()
    TriState.ENABLED_NOT -> !predicate()
}
