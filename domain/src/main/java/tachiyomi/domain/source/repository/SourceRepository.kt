package tachiyomi.domain.source.repository

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.model.FilterList
import exh.metadata.metadata.RaisedSearchMetadata
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount

public typealias SourcePagingSource = PagingSource<Long, /*SY --> */ Pair<Manga, RaisedSearchMetadata?>/*SY <-- */>

/** The loaded sources as domain rows, plus the paged listings a source's browse screen shows. */
public interface SourceRepository {

    /** Every loaded source, as a flow that re-emits when the registry changes. */
    public fun getSources(): Flow<List<Source>>

    /** The loaded sources that fetch over HTTP, as a flow. */
    public fun getOnlineSources(): Flow<List<Source>>

    /** Each source that has favourites paired with how many; stubs for uninstalled ones. */
    public fun getSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    /** Each source that has manga outside the library paired with how many; stubs for uninstalled ones. */
    public fun getSourcesWithNonLibraryManga(): Flow<List<SourceWithCount>>

    /** Paged results of [query] with [filterList] on source [sourceId]. */
    public fun search(sourceId: Long, query: String, filterList: FilterList): SourcePagingSource

    /** Paged "popular" listing of source [sourceId]. */
    public fun getPopular(sourceId: Long): SourcePagingSource

    /** Paged "latest updates" listing of source [sourceId]. */
    public fun getLatest(sourceId: Long): SourcePagingSource
}
