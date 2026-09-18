package tachiyomi.data.source

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.MetadataMangasPage
import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.RaisedSearchMetadata
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope

/** Ids handed out by [localizingNtl] start here. */
internal const val LOCAL_ID_BASE: Long = 100L

/** A [NetworkToLocalManga] that assigns ids [LOCAL_ID_BASE], [LOCAL_ID_BASE] + 1, ... in list order. */
internal fun localizingNtl(): NetworkToLocalManga {
    val networkToLocalManga = mockk<NetworkToLocalManga>()
    coEvery { networkToLocalManga.invoke(any<List<Manga>>()) } answers {
        firstArg<List<Manga>>().mapIndexed { index, manga -> manga.copy(id = LOCAL_ID_BASE + index) }
    }
    return networkToLocalManga
}

/**
 * Swaps the global Injekt scope for one that serves [networkToLocalManga], the dependency
 * `BaseSourcePagingSource` resolves by default; [restore] puts the previous scope back.
 */
internal class InjektEnv(networkToLocalManga: NetworkToLocalManga = localizingNtl()) {
    private var previous: InjektScope? = null

    /** The registrar serving the paging sources built while installed. */
    val registrar: InjektRegistrar = mockk<InjektRegistrar> {
        every { getInstance<NetworkToLocalManga>(any()) } returns networkToLocalManga
    }

    fun install() {
        previous = Injekt
        Injekt = InjektScope(registrar)
    }

    fun restore() {
        val scope = previous ?: return
        Injekt = scope
        previous = null
    }
}

/** A mocked source with the given identity. */
internal fun mockSource(sourceId: Long = 1L, latest: Boolean = false, language: String = "en"): Source =
    mockk<Source> {
        every { id } returns sourceId
        every { name } returns "Source $sourceId"
        every { lang } returns language
        every { supportsLatest } returns latest
    }

/** A source manga whose title is its [url]. */
internal fun sManga(url: String): SManga = SManga(url = url, title = url)

/** A plain page of the manga at [urls]. */
internal fun pageOf(vararg urls: String, hasNextPage: Boolean = false): MangasPage =
    MangasPage(urls.map(::sManga), hasNextPage)

/** A metadata page of the manga at [urls], paired positionally with [metadata]. */
internal fun metadataPageOf(
    urls: List<String>,
    metadata: List<RaisedSearchMetadata>,
    hasNextPage: Boolean = false,
    nextKey: Long? = null,
): MetadataMangasPage = MetadataMangasPage(
    mangas = urls.map(::sManga),
    hasNextPage = hasNextPage,
    mangasMetadata = metadata,
    nextKey = nextKey,
)

/** A refresh load for [key]. */
internal fun refresh(key: Long? = null): PagingSource.LoadParams<Long> =
    PagingSource.LoadParams.Refresh(key = key, loadSize = 20, placeholdersEnabled = false)

/** An append load for [key]. */
internal fun append(key: Long): PagingSource.LoadParams<Long> =
    PagingSource.LoadParams.Append(key = key, loadSize = 20, placeholdersEnabled = false)

/** A paging result of the SY paging sources. */
internal typealias LoadedResult = PagingSource.LoadResult<Long, Pair<Manga, RaisedSearchMetadata?>>

/** A loaded page of the SY paging sources. */
internal typealias LoadedPage = PagingSource.LoadResult.Page<Long, Pair<Manga, RaisedSearchMetadata?>>

/** The loaded page, failing the test on any other result. */
internal fun LoadedResult.page(): LoadedPage = this as LoadedPage

/** The error of a failed load, failing the test on any other result. */
internal fun LoadedResult.error(): Throwable =
    (this as PagingSource.LoadResult.Error<Long, Pair<Manga, RaisedSearchMetadata?>>).throwable

/** The (url, id) of every loaded manga, in page order. */
internal fun LoadedPage.urlsAndIds(): List<Pair<String, Long>> = data.map { it.first.url to it.first.id }
