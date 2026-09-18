package tachiyomi.data.source

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import exh.metadata.metadata.RaisedSearchMetadata
import io.kotest.matchers.shouldBe
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga

internal class SourcePagingSourceKeyTest {
    private val env = InjektEnv()
    private val source = mockSource(sourceId = 1L)

    @BeforeEach
    fun setUp() {
        env.install()
    }

    @AfterEach
    fun tearDown() {
        env.restore()
    }

    private fun state(
        anchorPosition: Int?,
        pages: List<LoadedPage>,
    ): PagingState<Long, Pair<Manga, RaisedSearchMetadata?>> = PagingState(
        pages = pages,
        anchorPosition = anchorPosition,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0,
    )

    private fun loadedPage(prevKey: Long?, nextKey: Long?): LoadedPage = PagingSource.LoadResult.Page(
        data = listOf(Manga.create().copy(url = "a") to null),
        prevKey = prevKey,
        nextKey = nextKey,
    )

    @Test
    fun noAnchorGivesNoKey() {
        SourcePopularPagingSource(source).getRefreshKey(state(null, listOf(loadedPage(1L, 3L)))) shouldBe null
    }

    @Test
    fun emptyPagesGiveNoKey() {
        val empty: LoadedPage = PagingSource.LoadResult.Page(data = emptyList(), prevKey = 1L, nextKey = 3L)

        SourcePopularPagingSource(source).getRefreshKey(state(0, listOf(empty))) shouldBe null
    }

    @Test
    fun prevKeyWins() {
        SourcePopularPagingSource(source).getRefreshKey(state(0, listOf(loadedPage(1L, 3L)))) shouldBe 1L
    }

    @Test
    fun nextKeyIsTheFallback() {
        SourcePopularPagingSource(source).getRefreshKey(state(0, listOf(loadedPage(null, 3L)))) shouldBe 3L
    }

    @Test
    fun noKeysGiveNoKey() {
        SourcePopularPagingSource(source).getRefreshKey(state(0, listOf(loadedPage(null, null)))) shouldBe null
    }

    @Test
    fun dependencyComesFromInjekt() {
        SourcePopularPagingSource(source)

        verify(exactly = 1) { env.registrar.getInstance<Any>(any()) }
    }
}
