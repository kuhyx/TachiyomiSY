package exh.smartsearch

import mihon.feature.migration.list.search.BaseSmartSearchEngine
import tachiyomi.domain.library.model.LibraryManga

// A library title must match the query this closely to count.
private const val LIBRARY_MATCH_THRESHOLD = 0.7

internal class SmartLibrarySearchEngine(
    extraSearchParams: String? = null,
) : BaseSmartSearchEngine<LibraryManga>(extraSearchParams, LIBRARY_MATCH_THRESHOLD) {

    override fun getTitle(result: LibraryManga) = result.manga.ogTitle

    suspend fun smartSearch(library: List<LibraryManga>, title: String): LibraryManga? =
        deepSearch(
            { query ->
                library.filter { it.manga.ogTitle.contains(query, true) }
            },
            title,
        )
}
