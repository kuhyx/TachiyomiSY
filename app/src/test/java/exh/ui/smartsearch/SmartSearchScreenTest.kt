package exh.ui.smartsearch

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreen
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

private const val WAIT_MS = 20_000L

@RunWith(RobolectricTestRunner::class)
internal class SmartSearchScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val networkToLocalManga = mockk<NetworkToLocalManga>()
    private val source = mockk<Source> {
        every { id } returns 3L
        every { name } returns "Src"
        every { getFilterList() } returns FilterList()
    }

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { networkToLocalManga }
                    single<SourceManager> { mockk { every { getOrStub(3L) } returns source } }
                },
            )
        }
        coEvery { networkToLocalManga(any<Manga>()) } answers { firstArg<Manga>().copy(id = 42L) }
    }

    @After
    fun tearDown() = stopKoin()

    private fun answer(block: () -> List<SManga>) {
        coEvery { source.getSearchManga(any(), any(), any()) } answers { MangasPage(block(), false) }
    }

    // Any screen the search navigates to is shown by name only; composing it would need the whole app.
    private fun showAndAwait(): String {
        compose.setContent {
            MaterialTheme {
                Navigator(SmartSearchScreen(3L, SourcesScreen.SmartSearchConfig(origTitle = "Needle"))) { navigator ->
                    if (navigator.lastItem is SmartSearchScreen) {
                        CurrentScreen()
                    } else {
                        Text("at ${navigator.lastItem::class.simpleName}")
                    }
                }
            }
        }
        val marker = hasText("at ", substring = true)
        compose.waitUntil(WAIT_MS) { compose.onAllNodes(marker).fetchSemanticsNodes().isNotEmpty() }
        return compose.onAllNodes(marker).fetchSemanticsNodes().single().config[SemanticsProperties.Text].joinToString()
    }

    @Test
    fun aMatchOpensTheManga() {
        answer { listOf(SManga.create().apply { title = "Needle" }) }
        showAndAwait() shouldBe "at MangaScreen"
    }

    @Test
    fun noMatchBrowsesTheSource() {
        answer { emptyList() }
        showAndAwait() shouldBe "at BrowseSourceScreen"
        ShadowToast.getTextOfLatestToast() shouldBe "Couldn't find the entry in the source!"
    }

    @Test
    fun anErrorBrowsesTheSource() {
        answer { error("boom") }
        showAndAwait() shouldBe "at BrowseSourceScreen"
        ShadowToast.getTextOfLatestToast() shouldBe "Error performing automatic search!"
    }

    @Test
    fun theSearchIsShownWhileRunning() {
        val gate = CompletableDeferred<List<SManga>>()
        coEvery { source.getSearchManga(any(), any(), any()) } coAnswers { MangasPage(gate.await(), false) }
        compose.setContent {
            MaterialTheme {
                Navigator(SmartSearchScreen(3L, SourcesScreen.SmartSearchConfig(origTitle = "Needle")))
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Searching source…").assertExists()
        compose.onNodeWithText("Src").assertExists()
        gate.cancel()
    }
}
