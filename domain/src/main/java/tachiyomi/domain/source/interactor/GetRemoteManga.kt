package tachiyomi.domain.source.interactor

import eu.kanade.tachiyomi.source.model.FilterList
import tachiyomi.domain.source.repository.SourcePagingSource
import tachiyomi.domain.source.repository.SourceRepository

/** Picks the paged listing a source's browse screen shows for a query. */
public class GetRemoteManga(
    private val repository: SourceRepository,
) {

    /**
     * The popular listing for [QUERY_POPULAR], the latest listing for [QUERY_LATEST], otherwise a
     * search of source [sourceId] for [query] with [filterList].
     */
    public operator fun invoke(sourceId: Long, query: String, filterList: FilterList): SourcePagingSource {
        return when (query) {
            QUERY_POPULAR -> repository.getPopular(sourceId)
            QUERY_LATEST -> repository.getLatest(sourceId)
            else -> repository.search(sourceId, query, filterList)
        }
    }

    /** The sentinel queries that select a listing instead of a search. */
    public companion object {
        /** Query value that selects the source's popular listing. */
        public const val QUERY_POPULAR: String = "eu.kanade.domain.source.interactor.POPULAR"

        /** Query value that selects the source's latest-updates listing. */
        public const val QUERY_LATEST: String = "eu.kanade.domain.source.interactor.LATEST"
    }
}
