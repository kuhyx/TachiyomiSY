package eu.kanade.tachiyomi.ui.manga

import eu.kanade.domain.manga.interactor.GetPagePreviews
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.source.local.LocalSource

@RunWith(RobolectricTestRunner::class)
internal class MangaInfoEditTest {
    private val harness = MangaHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun remoteEditsBecomeCustomInfo() {
        harness.mangaFlow.value = manga(favorite = true).copy(ogGenre = listOf("a"), ogStatus = 1L) to emptyList()
        val model = harness.loaded()
        model.updateMangaInfo(" T ", " ", null, null, "d", listOf("b"), 2L)
        verify {
            harness.parts.setCustomMangaInfo.set(
                CustomMangaInfo(1L, "T", null, null, null, "d", listOf("b"), 2L),
            )
        }
        model.updateMangaInfo(null, null, "x", "u", null, listOf("a"), 1L)
        verify { harness.parts.setCustomMangaInfo.set(CustomMangaInfo(1L, null, null, "x", "u", null, null, null)) }
        model.updateMangaInfo(null, null, null, null, null, emptyList(), null)
        harness.loading().updateMangaInfo(null, null, null, null, null, null, null)
    }

    @Test
    fun localEditsAreWritten() {
        val local = mockk<LocalSource>(relaxed = true)
        every { harness.sourceManager.get(LocalSource.ID) } returns local
        harness.mangaFlow.value = manga(source = LocalSource.ID, favorite = true) to listOf(chapter(1L))
        val model = harness.loaded()
        model.updateMangaInfo(" Title ", "au", "ar", "th", "de", listOf("g"), 1L)
        verify { local.updateMangaInfo(any()) }
        coVerify(timeout = 5_000) { harness.updateManga.await(match { it.title == "Title" && it.genre == listOf("g") }) }
        model.updateMangaInfo(" ", null, null, null, null, emptyList(), null)
        coVerify(timeout = 5_000) { harness.updateManga.await(match { it.title == "/m/1" && it.status == null }) }
        model.updateMangaInfo(null, null, null, null, null, null, null)
    }

    @Test
    fun chapterItemsCarryDownloadState() {
        harness.queue.value = listOf(download(chapter(1L), Download.State.DOWNLOADING))
        every { harness.downloadManager.isChapterDownloaded(any(), any(), "/c/2", any(), any()) } returns true
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L), chapter(2L), chapter(3L))
        val items = harness.loaded().awaitSuccess().chapters
        items.map { it.downloadState } shouldBe
            listOf(Download.State.DOWNLOADING, Download.State.DOWNLOADED, Download.State.NOT_DOWNLOADED)
        items.all { it.showScanlator } shouldBe true
    }

    @Test
    fun localChaptersAreDownloaded() {
        harness.mangaFlow.value = manga(source = LocalSource.ID, favorite = true) to listOf(chapter(1L))
        harness.loaded().awaitSuccess().chapters.single().isDownloaded shouldBe true
    }

    @Test
    fun mergedChaptersNameTheirSource() {
        val sources = (7L..9L).map { sourceId -> mockk<Source>(relaxed = true) { every { id } returns sourceId } }
        val model = harness.loaded()
        val data = mergedData(manga().copy(id = 1L, source = 8L)).copy(sources = sources)
        val items = model.toChapterListItems(listOf(chapter(1L), chapter(2L).copy(mangaId = 4L)), manga(), data)
        verify { sources[1].name }
        items.size shouldBe 2
        model.toChapterListItems(listOf(chapter(1L)), manga(), data.copy(sources = sources.take(2)))
            .single().sourceName shouldBe null
    }

    @Test
    fun previewSourcesLoadPreviews() {
        val previewSource = mockk<PagePreviewSource>(relaxed = true, moreInterfaces = arrayOf(Source::class))
        every { harness.sourceManager.getOrStub(any()) } returns previewSource
        val preview = PagePreview(index = 1, imageUrl = "u", source = 7L)
        coEvery { harness.getPagePreviews.await(any(), any(), 1) } returns
            GetPagePreviews.Result.Success(listOf(preview), hasNextPage = false, pageCount = 1)
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        val model = harness.loaded()
        model.awaitSuccess { it.pagePreviewsState is PagePreviewState.Success }
        coEvery { harness.getPagePreviews.await(any(), any(), 1) } returns GetPagePreviews.Result.Unused
        model.getPagePreviews(manga(), previewSource)
        model.awaitSuccess { it.pagePreviewsState == PagePreviewState.Unused }
        val error = IllegalStateException("x")
        coEvery { harness.getPagePreviews.await(any(), any(), 1) } returns GetPagePreviews.Result.Error(error)
        model.getPagePreviews(manga(), previewSource)
        model.awaitSuccess { it.pagePreviewsState == PagePreviewState.Error(error) }
    }
}
