package tachiyomi.data.source

import tachiyomi.data.Feed_saved_search
import tachiyomi.domain.source.model.FeedSavedSearch

/** Domain models from the generated `feed_saved_search` rows (SY). */
public object FeedSavedSearchMapper {
    /** The [FeedSavedSearch] of a `feed_saved_search` row. */
    public fun map(row: Feed_saved_search): FeedSavedSearch = FeedSavedSearch(
        id = row._id,
        source = row.source,
        savedSearch = row.saved_search,
        global = row.global,
    )
}
