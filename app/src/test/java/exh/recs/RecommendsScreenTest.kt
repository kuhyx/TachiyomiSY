package exh.recs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.manga.clearVoyagerScopes
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import exh.recs.batch.RankedSearchResults
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.sy.SYMR

@RunWith(RobolectricTestRunner::class)
internal class RecommendsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val loaded = MutableStateFlow(true)
    private val merged = RecommendsScreen.Args.MergedSourceMangas(listOf(results("Linked", 7L), results("Free", null)))

    private val getManga = mockk<GetManga>()

    @Before
    fun setUp() {
        coEvery { getManga.await(4L) } returns Manga.create().copy(id = 4L, source = 7L, ogTitle = "Needle")
        every { getManga.subscribe(any<String>(), any()) } returns flowOf(null)
        val source = mockk<Source>(relaxed = true) {
            every { id } returns 7L
            every { name } returns "Plain"
        }
        val sourceManager = mockk<SourceManager> {
            every { isInitialized } returns loaded
            every { getOrStub(any()) } returns source
        }
        val networkToLocal = mockk<NetworkToLocalManga>()
        coEvery { networkToLocal(any<Manga>()) } answers { firstArg<Manga>().copy(id = 70L) }
        stopKoin()
        startKoin {
            modules(
                module {
                    single { getManga }
                    single { sourceManager }
                    single { networkToLocal }
                },
            )
        }
    }

    @After
    fun tearDown() {
        clearVoyagerScopes()
        stopKoin()
    }

    private fun results(name: String, sourceId: Long?): RankedSearchResults = RankedSearchResults(
        recSourceName = name,
        recSourceCategoryResId = SYMR.strings.similar_titles.resourceId,
        recAssociatedSourceId = sourceId,
        results = mapOf(SManga(url = "/$name", title = "Manga $name") to 1),
    )

    private fun show(args: RecommendsScreen.Args = merged) {
        compose.setContent { ScreenHost(RecommendsScreen(args)) }
    }

    private fun hold(label: String) {
        compose.waitForLabel(label)
        compose.onNodeWithText(label).performTouchInput { longClick() }
        compose.waitForIdle()
    }

    private fun click(label: String) {
        compose.waitForLabel(label)
        compose.onNodeWithText(label).performClick()
        compose.waitForIdle()
    }

    @Test
    fun aSpinnerShowsUntilSourcesLoad() {
        loaded.value = false
        show()
        compose.waitForIdle()
        compose.hasLabel("Common recommendations") shouldBe false
        loaded.value = true
        compose.waitForLabel("Common recommendations")
    }

    @Test
    fun linkedEntriesOpenTheManga() {
        show()
        click("Manga Linked")
        compose.waitForLabel("opened:MangaScreen")
    }

    @Test
    fun linkedHoldOpensTheManga() {
        show()
        hold("Manga Linked")
        compose.waitForLabel("opened:MangaScreen")
    }

    @Test
    fun freeEntriesSearchSources() {
        show()
        click("Manga Free")
        compose.waitForLabel("opened:SourcesScreen")
    }

    @Test
    fun freeHoldOpensTheWebView() {
        show()
        hold("Manga Free")
        shadowOf(compose.activity).nextStartedActivity.component?.className shouldBe
            WebViewActivity::class.java.name
    }

    @Test
    fun mergedSourcesBrowseResults() {
        show()
        click("Free")
        compose.waitForLabel("opened:BrowseRecommendsScreen")
    }

    @Test
    fun singleSourceBrowsesBySource() {
        show(RecommendsScreen.Args.SingleSourceManga(mangaId = 4L, sourceId = 7L))
        compose.waitForLabel("Similar to Needle")
        click("AniList")
        compose.waitForLabel("opened:BrowseRecommendsScreen")
    }

    @Test
    fun backPops() {
        show()
        compose.waitForLabel("Common recommendations")
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
        compose.hasLabel("Common recommendations") shouldBe true
    }

    @Test
    fun libraryRowsReplaceResults() {
        every { getManga.subscribe(any<String>(), any()) } answers {
            val url = firstArg<String>()
            flowOf(Manga.create().copy(url = url, source = secondArg(), ogTitle = "Resolved $url"))
        }
        show()
        compose.waitForLabel("Resolved /Linked")
    }
}
