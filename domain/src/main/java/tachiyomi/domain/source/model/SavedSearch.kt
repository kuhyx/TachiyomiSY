package tachiyomi.domain.source.model

/**
 * A search the user saved on a source's browse screen: query text plus serialized filters (SY).
 *
 * @property id Row id; unique.
 * @property source Id of the source the search runs on.
 * @property name User-given name of the search.
 * @property query Search text, or null when the search is filters only.
 * @property filtersJson Filter state as a JSON array, or null when no filters were saved.
 */
public data class SavedSearch(
    // Tag identifier, unique
    val id: Long,

    // The source the saved search is for
    val source: Long,

    // If false the manga will not grab chapter updates
    val name: String,

    // The query if there is any
    val query: String?,

    // The filter list
    val filtersJson: String?,
)
