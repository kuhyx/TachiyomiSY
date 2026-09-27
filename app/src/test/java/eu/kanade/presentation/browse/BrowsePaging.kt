package eu.kanade.presentation.browse

import android.os.Looper
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import exh.md.utils.MangaDexRelation
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.RankedSearchMetadata
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.rules.ExternalResource
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.manga.model.Manga
import kotlin.coroutines.ContinuationInterceptor

/** One browse row as the source screens page it. */
internal typealias BrowseEntry = StateFlow<Pair<Manga, RaisedSearchMetadata?>>

internal val NotLoading: LoadState = LoadState.NotLoading(endOfPaginationReached = false)

/** Manga [id] titled "Title [id]". */
internal fun browseManga(id: Long, favorite: Boolean = false): Manga =
    Manga.create().copy(id = id, ogTitle = "Title $id", favorite = favorite)

/** A MangaDex row followed as [follow] (an index into the follow options) with [relation]. */
internal fun mangaDexMeta(follow: Int?, relation: MangaDexRelation?): RaisedSearchMetadata =
    MangaDexSearchMetadata().apply {
        followStatus = follow
        this.relation = relation
    }

/** A ranked-listing row at [rank]. */
internal fun rankedMeta(rank: Int?): RaisedSearchMetadata = RankedSearchMetadata().apply { this.rank = rank }

/** The browse rows for [pairs]. */
internal fun browseEntries(vararg pairs: Pair<Manga, RaisedSearchMetadata?>): List<BrowseEntry> =
    pairs.map { MutableStateFlow(it) }

/** [items] as one fixed page reporting the given load states. */
internal fun staticPages(
    items: List<BrowseEntry>,
    refresh: LoadState = NotLoading,
    prepend: LoadState = NotLoading,
    append: LoadState = NotLoading,
): Flow<PagingData<BrowseEntry>> = flowOf(PagingData.from(items, LoadStates(refresh, prepend, append)))

/** [items] followed by [after] placeholders, so the list asks for rows it has no data for. */
internal fun placeholderPages(items: List<BrowseEntry>, after: Int): Flow<PagingData<BrowseEntry>> =
    Pager(PagingConfig(pageSize = 5, enablePlaceholders = true)) { FixedSource(items, after) }.flow

/** A first page that never arrives: the refresh stays loading. */
internal fun stuckPages(): Flow<PagingData<BrowseEntry>> = Pager(PagingConfig(pageSize = 5)) { StuckSource() }.flow

private class FixedSource(private val items: List<BrowseEntry>, private val after: Int) :
    PagingSource<Int, BrowseEntry>() {
    override fun getRefreshKey(state: PagingState<Int, BrowseEntry>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BrowseEntry> =
        LoadResult.Page(data = items, prevKey = null, nextKey = null, itemsBefore = 0, itemsAfter = after)
}

private class StuckSource : PagingSource<Int, BrowseEntry>() {
    override fun getRefreshKey(state: PagingState<Int, BrowseEntry>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BrowseEntry> = awaitCancellation()
}

/** Waits until [condition] holds; on timeout the failure lists every text on screen. */
internal fun ComposeContentTestRule.waitForTexts(condition: () -> Boolean) {
    try {
        waitUntil(timeoutMillis = 10_000L, condition = condition)
    } catch (expected: ComposeTimeoutException) {
        val texts = onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }
        throw AssertionError("texts on screen: $texts\n${onRoot(useUnmergedTree = true).printToString()}", expected)
    }
}

/**
 * Clears what an earlier test left in the shared [AndroidUiDispatcher.Main], which paging-compose collects on.
 * Robolectric drops the main looper's queue between tests; a dispatch that was posted but never ran leaves the
 * dispatcher believing one is still scheduled, and it then never posts again, so no page ever arrives.
 */
internal fun resetUiDispatcher() {
    val dispatcher = AndroidUiDispatcher.Main[ContinuationInterceptor] as AndroidUiDispatcher
    val type = AndroidUiDispatcher::class.java
    fun field(name: String) = type.getDeclaredField(name).apply { isAccessible = true }
    field("scheduledTrampolineDispatch").setBoolean(dispatcher, false)
    field("scheduledFrameDispatch").setBoolean(dispatcher, false)
    (field("toRunTrampolined").get(dispatcher) as MutableCollection<*>).clear()
    (field("toRunOnFrame").get(dispatcher) as MutableCollection<*>).clear()
}

/**
 * Outermost rule for a class that pages: once the compose rule inside it has disposed the composition, runs what
 * that left on the main looper and resets [AndroidUiDispatcher.Main], so the next class in the fork starts clean.
 */
internal class UiDispatcherReset : ExternalResource() {
    override fun after() {
        shadowOf(Looper.getMainLooper()).idle()
        resetUiDispatcher()
    }
}
