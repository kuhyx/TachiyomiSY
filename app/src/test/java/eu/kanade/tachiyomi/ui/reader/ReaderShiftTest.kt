package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.useAutoWebtoon
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

/** Double-page shifting kept across chapter loads and toggled from the menu, and the auto-webtoon notice. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderShiftTest {
    private var harness = ReaderActivityHarness(pageCount = 4)

    private val ReaderActivity.pager get() = viewModel.state.value.viewer as PagerViewer

    @After
    fun tearDown() = harness.stop()

    private fun launch(flags: Long = 0L, genres: List<String>? = null): ReaderActivity {
        harness = ReaderActivityHarness(pageCount = 4, viewerFlags = flags)
        harness.start()
        if (genres != null) {
            coEvery { harness.vm.getManga.await(10L) } returns harness.vm.manga.copy(ogGenre = genres)
        }
        return harness.launch().get()
    }

    @Test
    fun remembersTheShiftedPage() {
        val activity = launch()
        val chapters = activity.viewModel.state.value.viewerChapters!!
        activity.viewModel.updateState { it.copy(indexChapterToShift = 2L, indexPageToShift = 1) }
        activity.setChapters(chapters)
        activity.viewModel.state.value.indexChapterToShift shouldBe null
        activity.viewModel.updateState { it.copy(indexChapterToShift = 2L, indexPageToShift = 99) }
        activity.setChapters(chapters)
        activity.viewModel.state.value.indexPageToShift shouldBe null
    }

    @Test
    fun lastShiftFollowsRequestedPage() {
        val activity = launch()
        val chapters = activity.viewModel.state.value.viewerChapters!!
        activity.viewModel.updateState { it.copy(lastShiftDoubleState = true) }
        chapters.currChapter.requestedPage = 1
        activity.setChapters(chapters)
        activity.pager.config.shiftDoublePage shouldBe true
        chapters.currChapter.pages!!.first().fullPage = true
        chapters.currChapter.requestedPage = 2
        activity.setChapters(chapters)
        activity.pager.config.shiftDoublePage shouldBe true
    }

    @Test
    fun menuTogglesTheShift() {
        val activity = launch()
        val before = activity.pager.config.shiftDoublePage
        activity.shiftDoublePages()
        activity.pager.config.shiftDoublePage shouldBe !before
        activity.viewModel.updateState { it.copy(viewerChapters = null) }
        activity.shiftDoublePages()
        activity.pager.config.shiftDoublePage shouldBe before
    }

    @Test
    fun webtoonHasNoShift() {
        val activity = launch(ReadingMode.WEBTOON.flagValue.toLong())
        activity.shiftDoublePages()
        activity.isFinishing shouldBe false
    }

    @Test
    fun webtoonEntriesAnnounceAutoMode() {
        ShadowToast.reset()
        val activity = launch(genres = listOf("Webtoon"))
        harness.vm.readerPreferences.useAutoWebtoon.get() shouldBe true
        ShadowToast.getTextOfLatestToast() shouldBe "Reading webtoon style"
        activity.isFinishing shouldBe false
    }
}
