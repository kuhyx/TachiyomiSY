package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.os.Looper
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** The last chapter, whose next transition leads nowhere: nothing to preload at its end. */
@RunWith(RobolectricTestRunner::class)
internal class PagerLastChapterTest {
    private val harness = ReaderActivityHarness(
        pageCount = 1,
        viewerFlags = ReadingMode.LEFT_TO_RIGHT.flagValue.toLong(),
    )
    private lateinit var activity: ReaderActivity
    private lateinit var viewer: PagerViewer

    @Before
    fun setUp() {
        harness.start()
        activity = harness.launch(chapterId = 3L).get()
        viewer = activity.viewModel.state.value.viewer as PagerViewer
    }

    @After
    fun tearDown() {
        harness.stop()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun lastOnly(): ViewerChapters {
        val chapters = activity.viewModel.state.value.viewerChapters!!
        return ViewerChapters(chapters.currChapter, chapters.prevChapter, null)
    }

    @Test
    fun idleWithoutNextChapter() {
        val chapters = lastOnly()
        viewer.isIdle = false
        viewer.setChapters(chapters)
        viewer.isIdle = true
        viewer.adapter.nextTransition?.to shouldBe null
    }

    @Test
    fun lastPageHasNothingToPreload() {
        val chapters = lastOnly()
        viewer.setChaptersInternal(chapters)
        val page = chapters.currChapter.pages!!.last()
        viewer.onReaderPageSelected(page, allowPreload = true, forward = true, hasExtraPage = false)
        viewer.adapter.currentChapter shouldBe chapters.currChapter
    }

    @Test
    fun strangerPageIsNoPreload() {
        viewer.setChaptersInternal(lastOnly())
        viewer.currentPage = ReaderPage(0).also { it.chapter = readerChapter(id = 51L) }
        viewer.checkAllowPreload(ReaderPage(0).also { it.chapter = readerChapter(id = 50L) }) shouldBe false
    }
}
