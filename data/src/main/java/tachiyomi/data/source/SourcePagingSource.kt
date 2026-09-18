package tachiyomi.data.source

import androidx.paging.PagingState
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.MetadataMangasPage
import exh.metadata.metadata.RaisedSearchMetadata
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.repository.SourcePagingSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Pages a source's search results for a query and filter list. */
public class SourceSearchPagingSource(
    source: Source,
    private val query: String,
    private val filters: FilterList,
) : BaseSourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage =
        source!!.getSearchManga(currentPage, query, filters)
}

/** Pages a source's popular listing. */
public class SourcePopularPagingSource(source: Source) : BaseSourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage = source!!.getPopularManga(currentPage)
}

/** Pages a source's latest-updates listing. */
public class SourceLatestPagingSource(source: Source) : BaseSourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage = source!!.getLatestUpdates(currentPage)
}

/**
 * Pages a source listing into local manga rows: each page is fetched with [requestNextPage], deduplicated
 * by url across pages and upserted through [NetworkToLocalManga]. An empty page ends with
 * [NoResultsException]; any other failure becomes a paging error.
 */
public abstract class BaseSourcePagingSource(
    protected open val source: Source?,
    protected val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
) : SourcePagingSource() {

    private val seenManga = hashSetOf<String>()

    /** The source's page [currentPage] (1-based) of this listing; throws on a source failure. */
    public abstract suspend fun requestNextPage(currentPage: Int): MangasPage

    override suspend fun load(
        params: LoadParams<Long>,
    ): LoadResult<Long, /*SY --> */ Pair<Manga, RaisedSearchMetadata?>/*SY <-- */> {
        val page = params.key ?: 1

        return try {
            val mangasPage = withIOContext {
                requestNextPage(page.toInt())
                    .takeIf { it.mangas.isNotEmpty() }
                    ?: throw NoResultsException()
            }

            // SY -->
            getPageLoadResult(params, mangasPage)
            // SY <--
        } catch (expected: Exception) {
            // Any source failure becomes a paging error the UI shows.
            LoadResult.Error(expected)
        }
    }

    // SY -->

    /**
     * The paging page for [mangasPage]: unseen manga mapped to local rows, paired with their search
     * metadata when the source provides it (SY), and a next key only while the source has more pages.
     */
    public open suspend fun getPageLoadResult(
        params: LoadParams<Long>,
        mangasPage: MangasPage,
    ): LoadResult.Page<Long, /*SY --> */ Pair<Manga, RaisedSearchMetadata?>/*SY <-- */> {
        val page = params.key ?: 1

        // SY -->
        val metadata = if (mangasPage is MetadataMangasPage) {
            mangasPage.mangasMetadata
        } else {
            emptyList()
        }

        val manga = mangasPage.mangas
            .mapIndexed { index, sManga -> sManga.toDomainManga(source!!.id) to metadata.getOrNull(index) }
            .filter { seenManga.add(it.first.url) }
            .let { manga ->
                manga.zip(networkToLocalManga(manga.map { it.first })).map { it.second to it.first.second }
            }
        // SY <--

        return LoadResult.Page(
            data = manga,
            prevKey = null,
            nextKey = if (mangasPage.hasNextPage) page + 1 else null,
        )
    }
    // SY <--

    override fun getRefreshKey(
        state: PagingState<Long, /*SY --> */ Pair<Manga, RaisedSearchMetadata?>/*SY <-- */>,
    ): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}

/** Thrown when a listing page comes back empty; the UI renders it as "no results found". */
public class NoResultsException : Exception()
