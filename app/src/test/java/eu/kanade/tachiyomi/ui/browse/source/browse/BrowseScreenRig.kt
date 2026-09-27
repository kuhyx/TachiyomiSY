package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.paging.PagingSource
import androidx.paging.PagingState
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import exh.metadata.metadata.RaisedSearchMetadata
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import mihon.domain.migration.usecases.MigrateMangaUseCase
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import tachiyomi.domain.manga.model.Manga

/** One page holding [items], or a load failure when [failure] is set. */
internal class ListPagingSource(
    private val items: List<Manga>,
    private val failure: Throwable? = null,
) : PagingSource<Long, Pair<Manga, RaisedSearchMetadata?>>() {
    override fun getRefreshKey(state: PagingState<Long, Pair<Manga, RaisedSearchMetadata?>>): Long? = null

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Pair<Manga, RaisedSearchMetadata?>> =
        failure?.let { LoadResult.Error(it) }
            ?: LoadResult.Page(items.map { it to null }, prevKey = null, nextKey = null)
}

/** A manga the browsed source lists. */
internal fun listed(id: Long, favorite: Boolean = false): Manga =
    Manga.create().copy(id = id, url = "/m/$id", ogTitle = "Manga $id", source = 1L, favorite = favorite)

/**
 * [BrowseSourceScreen] composed whole over [BrowseSourceHarness], listing [items]; screens it pushes show
 * up as `opened:<ClassName>`.
 */
internal class BrowseScreenRig(private val compose: ComposeContentTestRule) {
    val harness: BrowseSourceHarness = BrowseSourceHarness()
    val loaded: MutableStateFlow<Boolean> = MutableStateFlow(true)
    var items: List<Manga> = listOf(listed(1L))
    var failure: Throwable? = null

    fun start() {
        resetUiDispatcher()
        every { harness.sourceManager.isInitialized } returns loaded
        every { harness.getRemoteManga(any(), any(), any()) } answers { ListPagingSource(items, failure) }
        every { harness.getManga.subscribe(any<String>(), any()) } returns flowOf(null)
        harness.start()
        loadKoinModules(
            module {
                single { mockk<DownloadManager>(relaxed = true) }
                single { mockk<MigrateMangaUseCase>(relaxed = true) }
            },
        )
    }

    fun stop() = harness.stop()

    fun show(screen: Screen = BrowseSourceScreen(1L, "")) {
        compose.setContent { ScreenHost(screen) }
        compose.waitForIdle()
    }

    fun await(label: String) = compose.pollLabel(label)

    /** Clicks [label] through its clickable's semantics action; when the label repeats, the last match. */
    fun click(label: String) {
        val matcher = (hasText(label) or hasContentDescription(label)) and hasClickAction()
        val clickable = compose.onAllNodes(matcher)
        if (clickable.fetchSemanticsNodes().isNotEmpty()) {
            clickable.onLast().performSemanticsAction(SemanticsActions.OnClick)
        } else {
            compose.onAllNodes(hasText(label) or hasContentDescription(label), useUnmergedTree = true).onLast()
                .performClick()
        }
        compose.waitForIdle()
    }
}
