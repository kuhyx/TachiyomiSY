package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.source.Source
import exh.md.utils.MangaDexRelation
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.flow.Flow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.library.model.LibraryDisplayMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class BrowseSourceItemsTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        resetUiDispatcher()
        koin.start()
    }

    @After
    fun tearDown() = koin.stop()

    private fun entries() = browseEntries(
        browseManga(1L, favorite = true) to mangaDexMeta(follow = 1, relation = MangaDexRelation.SIMILAR),
        browseManga(2L) to mangaDexMeta(follow = 99, relation = null),
        browseManga(3L) to mangaDexMeta(follow = null, relation = null),
        browseManga(4L) to rankedMeta(rank = 7),
        browseManga(5L) to rankedMeta(rank = null),
        browseManga(6L) to null,
    )

    private fun show(pages: Flow<PagingData<BrowseEntry>>, mode: LibraryDisplayMode, source: Source? = null) {
        compose.setContent {
            MaterialTheme {
                BrowseSourceContent(
                    source = source,
                    mangaList = pages.collectAsLazyPagingItems(),
                    columns = GridCells.Fixed(2),
                    ehentaiBrowseDisplayMode = true,
                    displayMode = mode,
                    snackbarHostState = SnackbarHostState(),
                    contentPadding = PaddingValues(),
                    onWebViewClick = null,
                    onHelpClick = null,
                    onLocalSourceHelpClick = null,
                    onMangaClick = { events += "click ${it.id}" },
                    onMangaLongClick = { events += "long ${it.id}" },
                )
            }
        }
        compose.waitForTexts { compose.onAllNodes(hasText("Title 1")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun clickFirst() {
        compose.onNodeWithText("Title 1").performClick()
        compose.onNodeWithText("Title 1").performTouchInput { longClick() }
        events shouldContainExactly listOf("click 1", "long 1")
    }

    private fun badges() {
        compose.onNodeWithText("Reading").assertExists()
        compose.onNodeWithText("+7").assertExists()
    }

    @Test
    fun comfortableGrid() {
        val pages = staticPages(entries(), prepend = LoadState.Loading, append = LoadState.Loading)
        show(pages, LibraryDisplayMode.ComfortableGrid)
        badges()
        clickFirst()
    }

    @Test
    fun compactGrid() {
        show(staticPages(entries(), LoadState.Loading, prepend = LoadState.Loading), LibraryDisplayMode.CompactGrid)
        badges()
        clickFirst()
    }

    @Test
    fun coverOnlyGridPlaceholders() {
        show(placeholderPages(entries(), after = 3), LibraryDisplayMode.CoverOnlyGrid)
        clickFirst()
    }

    @Test
    fun listWithLoadingEnds() {
        val pages = staticPages(entries(), prepend = LoadState.Loading, append = LoadState.Loading)
        show(pages, LibraryDisplayMode.List)
        badges()
        clickFirst()
    }

    @Test
    fun listPlaceholders() {
        show(placeholderPages(entries(), after = 3), LibraryDisplayMode.List, source = plainBrowseSource())
        clickFirst()
    }

    @Test
    fun gridsWithoutLoading() {
        show(placeholderPages(entries(), after = 2), LibraryDisplayMode.ComfortableGrid)
        compose.onNodeWithText("Title 6").assertExists()
    }

    @Test
    fun compactPlaceholders() {
        show(placeholderPages(entries(), after = 2), LibraryDisplayMode.CompactGrid, source = plainBrowseSource())
        compose.onNodeWithText("Title 6").assertExists()
    }
}

private fun plainBrowseSource(): Source = browseSource(9L)
