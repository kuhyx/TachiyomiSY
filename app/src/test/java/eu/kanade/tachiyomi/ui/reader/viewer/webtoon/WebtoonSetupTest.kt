package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.MotionEvent
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.navigationModeWebtoon
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The webtoon viewer's wiring: the end of the last chapter, side tap regions, and a theme change. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonSetupTest {
    private val flags = ReadingMode.WEBTOON.flagValue.toLong()
    private var harness = ReaderActivityHarness(pageCount = 3, viewerFlags = flags)

    @After
    fun tearDown() = harness.stop()

    private fun launch(chapterId: Long = 2L): Pair<ReaderActivity, WebtoonViewer> {
        harness = ReaderActivityHarness(pageCount = 3, viewerFlags = flags)
        harness.start()
        val activity = harness.launch(chapterId).get()
        return activity to activity.viewModel.state.value.viewer as WebtoonViewer
    }

    @Test
    fun lastChapterEndShowsMenu() {
        val (activity, viewer) = launch(chapterId = 3L)
        activity.viewModel.state.value.viewerChapters?.nextChapter shouldBe null
        repeat(10) { viewer.recycler.scrollBy(0, 2000) }
        harness.settle()
        activity.viewModel.state.value.menuVisible shouldBe true
    }

    @Test
    fun firstChapterTopStaysPut() {
        val (activity, viewer) = launch(chapterId = 1L)
        viewer.recycler.scrollBy(0, 500)
        harness.settle()
        repeat(5) { viewer.recycler.scrollBy(0, -2000) }
        harness.settle()
        activity.viewModel.state.value.viewerChapters?.prevChapter shouldBe null
    }

    @Test
    fun sideRegionsScroll() {
        val (activity, viewer) = launch()
        harness.vm.readerPreferences.navigationModeWebtoon.set(4)
        harness.settle()
        listOf(0.1f, 0.9f).forEach { x ->
            val event = MotionEvent.obtain(
                0L,
                0L,
                MotionEvent.ACTION_UP,
                viewer.recycler.width * x,
                viewer.recycler.originalHeight * 0.5f,
                0,
            )
            viewer.recycler.tapListener!!.invoke(event)
        }
        activity.viewModel.state.value.menuVisible shouldBe false
    }

    @Test
    fun themeChangeRecreates() {
        val (activity, viewer) = launch()
        viewer.config.themeChangedListener!!.invoke()
        harness.settle()
        activity.isFinishing shouldBe false
    }
}
