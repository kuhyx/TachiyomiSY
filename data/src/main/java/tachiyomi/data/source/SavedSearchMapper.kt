package tachiyomi.data.source

import tachiyomi.data.Saved_search
import tachiyomi.domain.source.model.SavedSearch

/** Domain models from the generated `saved_search` rows (SY). */
public object SavedSearchMapper {
    /** The [SavedSearch] of a `saved_search` row. */
    public fun map(row: Saved_search): SavedSearch = SavedSearch(
        id = row._id,
        source = row.source,
        name = row.name,
        query = row.query,
        filtersJson = row.filters_json,
    )
}
