package tachiyomi.domain.source.model

import eu.kanade.tachiyomi.source.model.FilterList

/**
 * A [SavedSearch] with its filters restored against the source's live filter list, ready to
 * apply in the browse screen.
 *
 * @property id Row id of the [SavedSearch] this was built from.
 * @property name User-given name of the search.
 * @property query Search text, or null when the search is filters only.
 * @property filterList The source's filters with the saved values applied, or null when
 * they could not be restored.
 */
public data class EXHSavedSearch(
    val id: Long,
    val name: String,
    val query: String?,
    val filterList: FilterList?,
)
