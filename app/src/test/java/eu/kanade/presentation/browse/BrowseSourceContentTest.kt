package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.source.Source
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.mockkClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.StubSource
import tachiyomi.source.local.LocalSource

@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceContentTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val snackbar = SnackbarHostState()

    @Before
    fun setUp() {
        resetUiDispatcher()
        koin.start()
    }

    @After
    fun tearDown() = koin.stop()

    private fun show(pages: Flow<PagingData<BrowseEntry>>, source: Source? = null, withHelp: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                Column {
                    SnackbarHost(snackbar)
                    BrowseSourceContent(
                        source = source,
                        mangaList = pages.collectAsLazyPagingItems(),
                        columns = GridCells.Fixed(2),
                        ehentaiBrowseDisplayMode = false,
                        displayMode = LibraryDisplayMode.List,
                        snackbarHostState = snackbar,
                        contentPadding = PaddingValues(),
                        onWebViewClick = { events += "webview" }.takeIf { withHelp },
                        onHelpClick = { events += "help" }.takeIf { withHelp },
                        onLocalSourceHelpClick = { events += "local" },
                        onMangaClick = { events += "click ${it.id}" },
                        onMangaLongClick = { events += "long ${it.id}" },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun shows(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun loadingFirstPage() {
        show(stuckPages())
        compose.onNodeWithText("No results found").assertDoesNotExist()
    }

    @Test
    fun emptyOffersHelp() {
        show(staticPages(emptyList()))
        compose.waitForTexts { shows("No results found") }
        listOf("No results found", "Retry", "Open in WebView", "Help").forEach {
            compose.onNodeWithText(it).performClick()
        }
        events shouldContainExactly listOf("webview", "help")
    }

    @Test
    fun emptyWithoutHelp() {
        show(staticPages(emptyList(), refresh = LoadState.Error(Exception("boom"))), withHelp = false)
        compose.waitForTexts { shows("boom") }
        compose.onNodeWithText("Open in WebView").assertDoesNotExist()
    }

    @Test
    fun emptyLocalSourceGuide() {
        val local = mockkClass(LocalSource::class, relaxed = true)
        show(staticPages(emptyList(), append = LoadState.Error(IllegalStateException("late"))), source = local)
        compose.waitForTexts { shows("IllegalStateException: late") }
        compose.onNodeWithText("Local source guide").performClick()
        events shouldContainExactly listOf("local")
    }

    @Test
    fun loadErrorRetries() {
        val items = browseEntries(browseManga(1L) to null)
        show(staticPages(items, append = LoadState.Error(Exception("net"))))
        compose.waitForTexts { shows("Retry") }
        compose.onNodeWithText("Retry").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Title 1").performClick()
        events shouldContainExactly listOf("click 1")
    }

    @Test
    fun loadErrorDismissed() {
        val items = browseEntries(browseManga(1L) to null)
        show(staticPages(items, refresh = LoadState.Error(Exception("net"))))
        compose.waitForTexts { shows("net") }
        // An unconfined waiter takes the host the moment the retry snackbar lets go, so the retry sees it showing.
        val queued = CoroutineScope(Dispatchers.Unconfined).launch { snackbar.showSnackbar("queued") }
        compose.runOnIdle { snackbar.currentSnackbarData?.dismiss() }
        compose.waitForIdle()
        compose.onNodeWithText("net").assertDoesNotExist()
        compose.waitForTexts { queued.isCompleted }
    }

    @Test
    fun loadErrorDismissedAlone() {
        show(staticPages(browseEntries(browseManga(1L) to null), refresh = LoadState.Error(Exception("net"))))
        compose.waitForTexts { shows("net") }
        compose.runOnIdle { snackbar.currentSnackbarData?.dismiss() }
        compose.waitForIdle()
        compose.onNodeWithText("net").assertDoesNotExist()
    }

    @Test
    fun missingSourceScreen() {
        compose.setContent {
            MaterialTheme {
                MissingSourceScreen(source = StubSource(id = 5L, lang = "en", name = "Gone"), navigateUp = {})
            }
        }
        compose.onNodeWithText("Gone").assertExists()
    }

    @Test
    fun errorClearsOverRows() {
        val items = browseEntries(browseManga(1L) to null)
        val failing = LoadStates(NotLoading, NotLoading, LoadState.Error(Exception("net")))
        val pages = MutableStateFlow(PagingData.from(items, failing))
        show(pages)
        compose.waitForTexts { shows("net") }
        pages.value = PagingData.from(items, LoadStates(NotLoading, NotLoading, NotLoading))
        compose.waitForTexts { !shows("net") }
    }

    @Test
    fun galleryModeOffListsRows() {
        show(staticPages(browseEntries(browseManga(1L) to null)), source = browseSource(EH_SOURCE_ID))
        compose.waitForTexts { shows("Title 1") }
    }
}
