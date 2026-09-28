package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.presentation.browse.NotLoading
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.browse.browseEntries
import eu.kanade.presentation.browse.browseManga
import eu.kanade.presentation.browse.resetUiDispatcher
import eu.kanade.presentation.browse.staticPages
import eu.kanade.presentation.browse.waitForTexts
import eu.kanade.presentation.util.PresentationKoin
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The four browse layouts side by side, each holding a page while a refresh or the next page is loading,
 * or with nothing left to load.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class BrowseGridLoadingTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()

    @Before
    fun setUp() {
        resetUiDispatcher()
        koin.start()
    }

    @After
    fun tearDown() = koin.stop()

    private fun showAll(refresh: LoadState, append: LoadState) {
        fun pages() = staticPages(browseEntries(browseManga(1L) to null), refresh = refresh, append = append)
        val comfortable = pages()
        val compact = pages()
        val list = pages()
        val gallery = pages()
        compose.setContent {
            MaterialTheme {
                Column {
                    Column(Modifier.height(600.dp)) {
                        BrowseSourceComfortableGrid(
                            mangaList = comfortable.collectAsLazyPagingItems(),
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(),
                            onMangaClick = {},
                            onMangaLongClick = {},
                        )
                    }
                    Column(Modifier.height(600.dp)) {
                        BrowseSourceCompactGrid(
                            mangaList = compact.collectAsLazyPagingItems(),
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(),
                            onMangaClick = {},
                            onMangaLongClick = {},
                        )
                    }
                    Column(Modifier.height(600.dp)) {
                        BrowseSourceList(
                            mangaList = list.collectAsLazyPagingItems(),
                            contentPadding = PaddingValues(),
                            onMangaClick = {},
                            onMangaLongClick = {},
                        )
                    }
                    Column(Modifier.height(600.dp)) {
                        BrowseSourceEHentaiList(
                            mangaList = gallery.collectAsLazyPagingItems(),
                            contentPadding = PaddingValues(),
                            onMangaClick = {},
                            onMangaLongClick = {},
                        )
                    }
                }
            }
        }
        // The gallery list draws nothing for a row without gallery metadata, so three titles show.
        compose.waitForTexts { compose.onAllNodes(hasText("Title 1")).fetchSemanticsNodes().size == 3 }
        compose.waitForIdle()
    }

    @Test
    fun refreshingWithRows() = showAll(refresh = LoadState.Loading, append = NotLoading)

    @Test
    fun appendingWithRows() = showAll(refresh = NotLoading, append = LoadState.Loading)

    @Test
    fun idleWithRows() = showAll(refresh = NotLoading, append = NotLoading)
}
