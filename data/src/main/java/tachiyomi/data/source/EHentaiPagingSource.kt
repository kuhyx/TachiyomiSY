package tachiyomi.data.source

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.MetadataMangasPage
import exh.metadata.metadata.RaisedSearchMetadata
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.model.Manga

/**
 * [BaseSourcePagingSource] for E-Hentai based sources (SY): pages carry their own next key and every
 * manga is paired with its gallery metadata, without the url deduplication of the base class.
 */
public abstract class EHentaiPagingSource(
    override val source: Source,
) : BaseSourcePagingSource(source) {

    override suspend fun getPageLoadResult(
        params: LoadParams<Long>,
        mangasPage: MangasPage,
    ): LoadResult.Page<Long, Pair<Manga, RaisedSearchMetadata?>> {
        mangasPage as MetadataMangasPage
        val metadata = mangasPage.mangasMetadata

        val manga = mangasPage.mangas.map { it.toDomainManga(source.id) }
            .let { networkToLocalManga(it) }

        return LoadResult.Page(
            data = manga
                .mapIndexed { index, sManga -> sManga to metadata.getOrNull(index) },
            prevKey = null,
            nextKey = mangasPage.nextKey,
        )
    }
}

/**
 * Pages an E-Hentai based source's search results.
 *
 * @param source The source to search.
 * @property query The search text.
 * @property filters The search filters sent with the query.
 */
public class EHentaiSearchPagingSource(
    source: Source,
    public val query: String,
    public val filters: FilterList,
) : EHentaiPagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage =
        source.getSearchManga(currentPage, query, filters)
}

/** Pages an E-Hentai based source's popular listing. */
public class EHentaiPopularPagingSource(source: Source) : EHentaiPagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage = source.getPopularManga(currentPage)
}

/** Pages an E-Hentai based source's latest-updates listing. */
public class EHentaiLatestPagingSource(source: Source) : EHentaiPagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage = source.getLatestUpdates(currentPage)
}
