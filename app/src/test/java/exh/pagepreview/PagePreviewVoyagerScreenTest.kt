package exh.pagepreview

import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.manga.interactor.GetPagePreviews
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class PagePreviewVoyagerScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val getPagePreviews = mockk<GetPagePreviews>()
    private val getManga = mockk<GetManga>()
    private val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    private val source = mockk<Source>()
    private val manga = Manga.create().copy(id = MANGA_ID, source = 7L)
    private val chapter = Chapter.create().copy(id = CHAPTER_ID, mangaId = MANGA_ID)

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { getPagePreviews }
                    single { getManga }
                    single { getChaptersByMangaId }
                    single<SourceManager> { mockk<SourceManager> { every { getOrStub(any()) } returns source } }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
            )
        }
        coEvery { getManga.await(MANGA_ID) } returns manga
        coEvery { getChaptersByMangaId.await(MANGA_ID) } returns listOf(chapter)
        coEvery { getPagePreviews.await(manga, source, any()) } returns GetPagePreviews.Result.Success(
            pagePreviews = listOf(PagePreview(index = 1, imageUrl = "u", source = 7L)),
            hasNextPage = false,
            pageCount = 4,
        )
    }

    @After
    fun tearDown() = stopKoin()

    private fun actions(description: String): Int =
        compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().size

    private fun state() = PagePreviewState.Success(
        page = 1,
        pagePreviews = emptyList(),
        hasNextPage = false,
        pageCount = 4,
        manga = manga,
        chapter = chapter,
        source = source,
    )

    @Test
    fun theScreenWiresTheModel() {
        compose.setContent { MaterialTheme { Navigator(PagePreviewScreen(MANGA_ID)) } }
        compose.waitUntil(timeoutMillis = 10_000) { actions("Go to") == 1 }
        compose.onNodeWithText("Page previews").assertIsDisplayed()
        compose.onNodeWithContentDescription("Go to").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Go to").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Page previews").assertIsDisplayed()
    }

    // A plain application context refuses to start an activity, so the reader is launched from one.
    @Test
    fun openingAPageStartsTheReader() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        PagePreviewScreen(MANGA_ID).openPage(activity, state(), page = 3)
        val intent: Intent = shadowOf(activity).nextStartedActivity.shouldNotBeNull()
        intent.component?.className shouldBe ReaderActivity::class.java.name
        intent.extras?.getLong("manga") shouldBe MANGA_ID
        intent.extras?.getLong("chapter") shouldBe CHAPTER_ID
        intent.extras?.getInt("page") shouldBe 3
    }

    @Test
    fun otherStatesOpenNothing() {
        PagePreviewScreen(MANGA_ID).openPage(context, PagePreviewState.Loading, page = 1)
        PagePreviewScreen(MANGA_ID).openPage(context, PagePreviewState.Error(IllegalStateException()), page = 1)
        shadowOf(context).nextStartedActivity.shouldBeNull()
    }

    private object NoCustomInfo : CustomMangaRepository {
        override fun get(mangaId: Long): CustomMangaInfo? = null
        override fun set(mangaInfo: CustomMangaInfo) = Unit
    }

    private companion object {
        const val MANGA_ID = 12L
        const val CHAPTER_ID = 5L
    }
}
