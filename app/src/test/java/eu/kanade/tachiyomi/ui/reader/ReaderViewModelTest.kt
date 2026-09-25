package eu.kanade.tachiyomi.ui.reader

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import eu.kanade.tachiyomi.data.download.addDownloadsToStartOfQueue
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ReaderViewModelTest {

    private val harness = ReaderVmHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        harness.stop()
    }

    private fun ReaderViewModel.clear() {
        val method = ReaderViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(this)
    }

    @Test
    fun defaultsComeFromInjekt() {
        val vm = ReaderViewModel(SavedStateHandle())
        vm.readerPreferences shouldBe harness.readerPreferences
        vm.sourceManager shouldBe harness.sourceManager
        vm.images.imageSaver shouldBe harness.imageSaver
        vm.needsInit() shouldBe true
        vm.manga.shouldBeNull()
    }

    @Test
    fun savedStateRestoresPosition() {
        val saved = SavedStateHandle(mapOf<String, Any>("chapter_id" to 4L, "page_index" to 3))
        val vm = harness.viewModel(saved)
        vm.chapterId shouldBe 4L
        vm.chapterPageIndex shouldBe 3
        vm.chapterId = 5L
        vm.chapterPageIndex = 6
        saved.get<Long>("chapter_id") shouldBe 5L
        saved.get<Int>("page_index") shouldBe 6
    }

    @Test
    fun freshStateHasNoPosition() {
        val vm = harness.viewModel()
        vm.chapterId shouldBe -1L
        vm.chapterPageIndex shouldBe -1
        vm.state.value.currentChapter.shouldBeNull()
        vm.state.value.totalPages shouldBe -1
    }

    @Test
    fun stateCountsPages() {
        val chapter = readerChapter()
        loadedPages(chapter, count = 3)
        val state = ReaderViewModel.State(viewerChapters = ViewerChapters(chapter, null, null))
        state.currentChapter shouldBe chapter
        state.totalPages shouldBe 3
        ReaderViewModel.State(viewerChapters = ViewerChapters(readerChapter(), null, null)).totalPages shouldBe -1
    }

    @Test
    fun lazyListsReadTheManga() {
        harness.chapters(domainChapter(1L), domainChapter(2L))
        val vm = harness.loadedViewModel()
        vm.needsInit() shouldBe false
        vm.unfilteredChapterList.size shouldBe 2
        vm.unfilteredChapterList.size shouldBe 2
        vm.incognitoMode shouldBe false
        vm.incognitoMode shouldBe false
        verify(exactly = 1) { harness.getIncognitoState.await(1L) }
    }

    @Test
    fun clearWithoutChapters() {
        mockkStatic(QUEUE_KT)
        val vm = harness.viewModel()
        vm.chapterToDownload = mockk<Download>()
        vm.clear()
        verify(exactly = 0) { harness.downloadManager.addDownloadsToStartOfQueue(any()) }
    }

    @Test
    fun clearReleasesChapters() {
        val vm = harness.viewModel()
        val chapter = readerChapter()
        chapter.ref()
        chapter.state = ReaderChapter.State.Loading
        vm.updateState { it.copy(viewerChapters = ViewerChapters(chapter, null, null)) }
        mockkStatic(QUEUE_KT)
        every { harness.downloadManager.addDownloadsToStartOfQueue(any()) } returns Unit
        vm.clear()
        chapter.state shouldBe ReaderChapter.State.Wait
        verify(exactly = 0) { harness.downloadManager.addDownloadsToStartOfQueue(any()) }
        val download = mockk<Download>()
        chapter.ref()
        vm.chapterToDownload = download
        vm.clear()
        verify { harness.downloadManager.addDownloadsToStartOfQueue(listOf(download)) }
    }

    @Test
    fun sealedValueTypes() {
        val page = ReaderPage(0)
        ReaderViewModel.Dialog.PageActions(page).extraPage.shouldBeNull()
        ReaderViewModel.Dialog.PageActions(page, page).extraPage shouldBe page
        val uri = mockk<Uri>()
        ReaderViewModel.Event.ShareImage(uri, page).secondPage.shouldBeNull()
        ReaderViewModel.Event.CopyImage(uri).uri shouldBe uri
        ReaderViewModel.Event.SetOrientation(2).orientation shouldBe 2
        (ReaderViewModel.SaveImageResult.Success(uri) as ReaderViewModel.SaveImageResult).let {
            (it as ReaderViewModel.SaveImageResult.Success).uri shouldBe uri
        }
        val error = IllegalStateException("x")
        ReaderViewModel.SaveImageResult.Error(error).error shouldBe error
        ReaderViewModel.SetAsCoverResult.entries.size shouldBe 3
    }
}
