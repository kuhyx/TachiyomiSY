package exh.pagepreview

import eu.kanade.domain.manga.interactor.GetPagePreviews
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.domain.source.service.SourceManager

internal class PagePreviewScreenModelTest {
    private val getPagePreviews = mockk<GetPagePreviews>()
    private val getManga = mockk<GetManga>()
    private val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    private val source = mockk<Source>()
    private val sourceManager = mockk<SourceManager> { every { getOrStub(any()) } returns source }
    private val manga = Manga.create().copy(id = MANGA_ID, source = 7L)
    private val chapter = Chapter.create().copy(id = 3, mangaId = MANGA_ID, sourceOrder = 1)
    private val previews = listOf(PagePreview(index = 0, imageUrl = "u", source = 7L))

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        stopKoin()
        startKoin {
            modules(
                module {
                    single { getPagePreviews }
                    single { getManga }
                    single { getChaptersByMangaId }
                    single<SourceManager> { sourceManager }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
            )
        }
        coEvery { getManga.await(MANGA_ID) } returns manga
        coEvery { getChaptersByMangaId.await(MANGA_ID) } returns listOf(chapter, chapter.copy(id = 4, sourceOrder = 2))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    // The model loads on the real IO dispatcher, so its state is polled rather than scheduled.
    private fun PagePreviewScreenModel.settled(): PagePreviewState {
        val deadline = System.currentTimeMillis() + TIMEOUT_MILLIS
        var last = state.value
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(SLEEP_MILLIS)
            val current = state.value
            if (current != PagePreviewState.Loading && current == last) return current
            last = current
        }
        return last
    }

    private fun model() = PagePreviewScreenModel(
        mangaId = MANGA_ID,
        getPagePreviews = getPagePreviews,
        getManga = getManga,
        getChaptersByMangaId = getChaptersByMangaId,
        sourceManager = sourceManager,
    )

    @Test
    fun previewsBecomeSuccess() = runBlocking<Unit> {
        coEvery { getPagePreviews.await(manga, source, 1) } returns
            GetPagePreviews.Result.Success(previews, hasNextPage = true, pageCount = 3)
        val screenModel = model()
        screenModel.pageDialogOpen.shouldBeFalse()
        val state = screenModel.settled().shouldBeInstanceOf<PagePreviewState.Success>()
        state.page shouldBe 1
        state.pagePreviews shouldBe previews
        state.pageCount shouldBe 3
        state.manga shouldBe manga
        state.chapter shouldBe chapter
    }

    @Test
    fun movingToAPageUpdatesTheState() = runBlocking<Unit> {
        coEvery { getPagePreviews.await(manga, source, any()) } returns
            GetPagePreviews.Result.Success(previews, hasNextPage = true, pageCount = 3)
        val screenModel = model()
        // The page is a state flow, so the first load must land before the move conflates it away.
        screenModel.settled().shouldBeInstanceOf<PagePreviewState.Success>().page shouldBe 1
        val newPreviews = listOf(PagePreview(index = 5, imageUrl = "v", source = 7L))
        coEvery { getPagePreviews.await(manga, source, 2) } returns
            GetPagePreviews.Result.Success(newPreviews, hasNextPage = false, pageCount = 3)
        screenModel.moveToPage(2)
        val state = screenModel.settled().shouldBeInstanceOf<PagePreviewState.Success>()
        state.page shouldBe 2
        state.pagePreviews shouldBe newPreviews
        state.hasNextPage.shouldBeFalse()
    }

    @Test
    fun unusedSourcesKeepLoading() = runBlocking<Unit> {
        coEvery { getPagePreviews.await(manga, source, 1) } returns GetPagePreviews.Result.Unused
        val screenModel = model()
        screenModel.settled() shouldBe PagePreviewState.Loading
    }

    @Test
    fun errorsAndFailuresBecomeError() = runBlocking<Unit> {
        val failure = IllegalStateException("no previews")
        coEvery { getPagePreviews.await(manga, source, 1) } returns GetPagePreviews.Result.Error(failure)
        val failed = model()
        failed.settled().shouldBeInstanceOf<PagePreviewState.Error>().error shouldBe failure
        coEvery { getPagePreviews.await(manga, source, 1) } throws failure
        val thrown = model()
        thrown.settled().shouldBeInstanceOf<PagePreviewState.Error>().error shouldBe failure
    }

    @Test
    fun errorThenSuccessReplaces() = runBlocking<Unit> {
        coEvery { getPagePreviews.await(manga, source, 1) } returns
            GetPagePreviews.Result.Error(IllegalStateException("first"))
        val screenModel = model()
        screenModel.settled().shouldBeInstanceOf<PagePreviewState.Error>()
        coEvery { getPagePreviews.await(manga, source, 2) } returns
            GetPagePreviews.Result.Success(previews, hasNextPage = false, pageCount = null)
        screenModel.moveToPage(2)
        screenModel.settled().shouldBeInstanceOf<PagePreviewState.Success>().pageCount shouldBe null
    }

    @Test
    fun mangaWithoutChaptersIsAnError() = runBlocking<Unit> {
        coEvery { getChaptersByMangaId.await(MANGA_ID) } returns emptyList()
        val screenModel = model()
        screenModel.settled().shouldBeInstanceOf<PagePreviewState.Error>().error.message shouldBe "No chapters found"
    }

    @Test
    fun dialogStateIsRemembered() = runBlocking<Unit> {
        coEvery { getPagePreviews.await(manga, source, 1) } returns GetPagePreviews.Result.Unused
        val screenModel = model()
        screenModel.pageDialogOpen = true
        screenModel.pageDialogOpen shouldBe true
    }

    private object NoCustomInfo : CustomMangaRepository {
        override fun get(mangaId: Long): CustomMangaInfo? = null
        override fun set(mangaInfo: CustomMangaInfo) = Unit
    }

    private companion object {
        const val MANGA_ID = 12L
        const val TIMEOUT_MILLIS = 5_000L
        const val SLEEP_MILLIS = 20L
    }
}
