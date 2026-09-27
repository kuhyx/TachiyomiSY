package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Chapters handed to the activity when the viewer cannot shift pages, has no pages yet, or is gone. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderSetChaptersTest {
    private var harness = ReaderActivityHarness(pageCount = 4)

    @After
    fun tearDown() = harness.stop()

    private fun launch(mode: ReadingMode? = null): ReaderActivity {
        harness = ReaderActivityHarness(pageCount = 4, viewerFlags = mode?.flagValue?.toLong() ?: 0L)
        harness.start()
        return harness.launch().get()
    }

    @Test
    fun webtoonIgnoresShifts() {
        val activity = launch(ReadingMode.WEBTOON)
        val chapters = activity.viewModel.state.value.viewerChapters!!
        activity.viewModel.updateState { it.copy(indexChapterToShift = 2L, indexPageToShift = 1) }
        activity.setChapters(chapters)
        activity.viewModel.updateState { it.copy(indexChapterToShift = 2L, lastShiftDoubleState = true) }
        activity.setChapters(chapters)
        activity.viewModel.state.value.indexChapterToShift shouldBe 2L
        activity.onPageSelected(chapters.currChapter.pages!![0], hasExtraPage = true)
        activity.viewModel.state.value.currentPageText shouldBe "1-2"
    }

    @Test
    fun pendingChapterShiftsNothing() {
        val activity = launch()
        val pending = ViewerChapters(readerChapter(id = 7L), null, null)
        activity.viewModel.updateState { it.copy(indexChapterToShift = 7L, indexPageToShift = 0) }
        activity.setChapters(pending)
        activity.viewModel.updateState { it.copy(lastShiftDoubleState = true) }
        activity.setChapters(pending)
        activity.viewModel.state.value.indexPageToShift shouldBe null
    }

    @Test
    fun noViewerOrChapterIsQuiet() {
        val activity = launch()
        val chapters = activity.viewModel.state.value.viewerChapters!!
        val viewer = activity.viewModel.state.value.viewer
        activity.viewModel.updateState { it.copy(viewer = null) }
        activity.setChapters(chapters)
        activity.moveToPageIndex(1)
        activity.viewModel.updateState { it.copy(viewer = viewer, viewerChapters = null) }
        activity.moveToPageIndex(1)
        activity.viewModel.state.value.viewerChapters shouldBe null
    }
}
