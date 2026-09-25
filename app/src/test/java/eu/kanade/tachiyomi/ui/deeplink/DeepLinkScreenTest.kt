package eu.kanade.tachiyomi.ui.deeplink

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ResolvableSource
import eu.kanade.tachiyomi.source.online.UriType
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.customInfoModule
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import mihon.domain.source.interactor.UpdateMangaFromRemote
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
import tachiyomi.domain.chapter.interactor.GetChapterByUrlAndMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class DeepLinkScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val source = mockk<ResolvableSource> { every { id } returns 7L }
    private val sourceManager = mockk<SourceManager> { every { getAll() } returns listOf(source) }
    private val networkToLocalManga = mockk<NetworkToLocalManga>()
    private val getChapter = mockk<GetChapterByUrlAndMangaId>()
    private val manga = Manga.create().copy(id = 3L)

    @Before
    fun setUp() {
        startKoin {
            modules(
                customInfoModule(),
                module {
                    single { sourceManager }
                    single { networkToLocalManga }
                    single { getChapter }
                    single { mockk<UpdateMangaFromRemote>() }
                },
            )
        }
        coEvery { networkToLocalManga(any<Manga>()) } returns manga
        coEvery { source.getManga(any()) } returns SManga.create().also { it.url = "/m" }
        coEvery { source.getChapter(any()) } returns SChapter.create().also { it.url = "/c" }
        coEvery { getChapter.await("/c", 3L) } returns Chapter.create().copy(id = 9L)
    }

    @After
    fun tearDown() = stopKoin()

    private fun show(type: UriType) {
        every { source.getUriType(any()) } returns type
        compose.setContent { ScreenHost(DeepLinkScreen("q")) }
    }

    @Test
    fun loadingShowsTheSearchBar() {
        coEvery { source.getManga(any()) } coAnswers { awaitCancellation() }
        show(UriType.Manga)
        compose.onNodeWithText("Search…").assertExists()
    }

    @Test
    fun noResultsOpensGlobalSearch() {
        show(UriType.Unknown)
        compose.waitUntil(WAIT) { opened("GlobalSearchScreen") }
    }

    @Test
    fun mangaOpensTheMangaScreen() {
        show(UriType.Manga)
        compose.waitUntil(WAIT) { opened("MangaScreen") }
    }

    @Test
    fun chapterOpensTheReader() {
        show(UriType.Chapter)
        compose.waitUntil(WAIT) { shadowOf(compose.activity).peekNextStartedActivity() != null }
        val intent = shadowOf(compose.activity).nextStartedActivity
        intent.component?.className shouldBe ReaderActivity::class.java.name
    }

    @Test
    fun defaultQueryIsEmpty() {
        DeepLinkScreen().query shouldBe ""
    }

    private fun opened(name: String): Boolean =
        compose.onAllNodes(hasText("opened:$name")).fetchSemanticsNodes().isNotEmpty()
}

private const val WAIT = 10_000L
