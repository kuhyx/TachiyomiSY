package tachiyomi.data

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class QueryPagingSourceTest {
    private val driver = inMemoryDriver()
    private val database = databaseOn(driver)
    private val source = QueryPagingSource(
        countQuery = {
            Query(identifier = 1, queryKeys = KEYS, driver = driver, query = COUNT_SQL) { it.getLong(0)!! }
        },
        queryProvider = { limit, offset ->
            Query(identifier = 2, queryKeys = KEYS, driver = driver, query = pageSql(limit, offset)) {
                it.getString(0)!!
            }
        },
    )

    @BeforeEach
    fun seed() = runTest {
        // Five rows in sort order: the seeded system category "" plus a..d.
        listOf("a", "b", "c", "d").forEachIndexed { index, name -> database.seedCategory(name, order = index + 1L) }
    }

    private suspend fun page(params: LoadParams<Long>): LoadResult.Page<Long, String> =
        source.load(params) as LoadResult.Page<Long, String>

    @Test
    fun refreshWithoutKeyStartsAt0() = runTest {
        val page = page(LoadParams.Refresh(key = null, loadSize = 2, placeholdersEnabled = true))
        page.data shouldBe listOf("", "a")
        page.prevKey shouldBe null
        page.nextKey shouldBe 2L
        page.itemsBefore shouldBe 0
        page.itemsAfter shouldBe 3
        source.jumpingSupported shouldBe true
    }

    @Test
    fun refreshWithKeyPagesFromIt() = runTest {
        val page = page(LoadParams.Refresh(key = 2L, loadSize = 2, placeholdersEnabled = true))
        page.data shouldBe listOf("b", "c")
        page.prevKey shouldBe 2L
        page.nextKey shouldBe 4L
        page.itemsBefore shouldBe 2
        page.itemsAfter shouldBe 1
    }

    @Test
    fun appendLastPageHasNoNextKey() = runTest {
        val page = page(LoadParams.Append(key = 4L, loadSize = 2, placeholdersEnabled = true))
        page.data shouldBe listOf("d")
        page.prevKey shouldBe 2L
        page.nextKey shouldBe null
        page.itemsAfter shouldBe 0
    }

    @Test
    fun appendNearStartHasNoPrev() = runTest {
        val page = page(LoadParams.Append(key = 1L, loadSize = 2, placeholdersEnabled = true))
        page.data shouldBe listOf("a", "b")
        page.prevKey shouldBe null
        page.nextKey shouldBe 3L
    }

    @Test
    fun prependStepsBackByLoadSize() = runTest {
        val page = page(LoadParams.Prepend(key = 3L, loadSize = 2, placeholdersEnabled = true))
        page.data shouldBe listOf("a", "b")
        page.prevKey shouldBe 1L
        page.nextKey shouldBe 3L
        page.itemsBefore shouldBe 1
    }

    @Test
    fun queryFailureIsAnError() = runTest {
        val failing = QueryPagingSource<String>(
            countQuery = { error("no count") },
            queryProvider = { _, _ -> error("no page") },
        )
        val result = failing.load(LoadParams.Refresh(key = null, loadSize = 2, placeholdersEnabled = true))
        (result as LoadResult.Error<Long, String>).throwable.message shouldBe "no count"
    }

    @Test
    fun tableChangeInvalidates() = runTest {
        page(LoadParams.Refresh(key = null, loadSize = 2, placeholdersEnabled = true))
        page(LoadParams.Append(key = 2L, loadSize = 2, placeholdersEnabled = true))
        source.invalid shouldBe false
        driver.notifyListeners("categories")
        source.invalid shouldBe true
    }

    @Test
    fun invalidBeforeAnyLoad() {
        source.queryResultsChanged()
        source.invalid shouldBe true
    }

    @Test
    fun refreshKeyFollowsAnchor() {
        val page = LoadResult.Page(data = listOf("b", "c"), prevKey = 2L, nextKey = 4L)
        source.getRefreshKey(stateOf(page, anchorPosition = 1)) shouldBe 2L
        val first = LoadResult.Page(data = listOf("", "a"), prevKey = null, nextKey = 2L)
        source.getRefreshKey(stateOf(first, anchorPosition = 0)) shouldBe 2L
    }

    @Test
    fun refreshKeyNullWithoutAnchor() {
        val page = LoadResult.Page(data = listOf("b"), prevKey = 2L, nextKey = null)
        source.getRefreshKey(stateOf(page, anchorPosition = null)) shouldBe null
        val noPages = PagingState<Long, String>(
            pages = emptyList(),
            anchorPosition = 0,
            config = CONFIG,
            leadingPlaceholderCount = 0,
        )
        source.getRefreshKey(noPages) shouldBe null
    }

    private companion object {
        val KEYS = arrayOf("categories")
        val CONFIG = PagingConfig(pageSize = 2)
        const val COUNT_SQL = "SELECT count(*) FROM categories"

        fun pageSql(limit: Long, offset: Long): String =
            "SELECT name FROM categories ORDER BY sort LIMIT $limit OFFSET $offset"

        fun stateOf(page: LoadResult.Page<Long, String>, anchorPosition: Int?): PagingState<Long, String> =
            PagingState(
                pages = listOf(page),
                anchorPosition = anchorPosition,
                config = CONFIG,
                leadingPlaceholderCount = 0,
            )
    }
}
