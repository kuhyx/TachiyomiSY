package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.KeyEvent
import android.view.MotionEvent
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.readWithLongTap
import eu.kanade.tachiyomi.ui.reader.setting.readWithVolumeKeys
import eu.kanade.tachiyomi.ui.reader.setting.readWithVolumeKeysInverted
import eu.kanade.tachiyomi.ui.reader.showMenu
import eu.kanade.tachiyomi.ui.reader.viewer.pager.scrollEvent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
internal class WebtoonViewerTest {

    private val harness = ReaderActivityHarness(pageCount = 6, viewerFlags = ReadingMode.WEBTOON.flagValue.toLong())
    private lateinit var activity: ReaderActivity
    private lateinit var viewer: WebtoonViewer

    @Before
    fun setUp() {
        harness.start()
        activity = harness.launch().get()
        viewer = activity.viewModel.state.value.viewer as WebtoonViewer
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    private fun press(code: Int): Boolean {
        viewer.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        return viewer.handleKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    private fun tap(xFraction: Float, yFraction: Float) {
        val event = MotionEvent.obtain(
            0L,
            0L,
            MotionEvent.ACTION_UP,
            viewer.recycler.width * xFraction,
            viewer.recycler.originalHeight * yFraction,
            0,
        )
        viewer.recycler.tapListener!!.invoke(event)
    }

    @Test
    fun keysScroll() {
        listOf(
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
        ).forEach { press(it) shouldBe true }
        press(KeyEvent.KEYCODE_A) shouldBe false
        press(KeyEvent.KEYCODE_MENU) shouldBe true
        activity.viewModel.state.value.menuVisible shouldBe true
        viewer.handleGenericMotionEvent(scrollEvent(1f)) shouldBe false
    }

    @Test
    fun volumeKeysNeedPreference() {
        press(KeyEvent.KEYCODE_VOLUME_DOWN) shouldBe false
        harness.vm.readerPreferences.readWithVolumeKeys.set(true)
        harness.settle()
        press(KeyEvent.KEYCODE_VOLUME_DOWN) shouldBe true
        press(KeyEvent.KEYCODE_VOLUME_UP) shouldBe true
        harness.vm.readerPreferences.readWithVolumeKeysInverted.set(true)
        harness.settle()
        press(KeyEvent.KEYCODE_VOLUME_DOWN) shouldBe true
        press(KeyEvent.KEYCODE_VOLUME_UP) shouldBe true
        activity.showMenu()
        press(KeyEvent.KEYCODE_VOLUME_UP) shouldBe false
    }

    @Test
    fun tapsScrollAndToggle() {
        tap(0.5f, 0.1f)
        tap(0.5f, 0.9f)
        tap(0.5f, 0.5f)
        activity.viewModel.state.value.menuVisible shouldBe true
        viewer.config.usePageTransitions = true
        tap(0.5f, 0.1f)
        tap(0.5f, 0.9f)
        viewer.linearScroll(1.seconds)
    }

    @Test
    fun longTapNeedsPage() {
        val listener = viewer.recycler.longTapListener!!
        listener(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0)) shouldBe true
        activity.viewModel.state.value.dialog.shouldBeInstanceOf<ReaderViewModel.Dialog.PageActions>()
        harness.vm.readerPreferences.readWithLongTap.set(false)
        harness.settle()
        listener(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0)) shouldBe false
        activity.showMenu()
        listener(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0)) shouldBe true
        listener(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, -5f, -5f, 0)) shouldBe false
    }

    @Test
    fun transitionsAndPages() {
        val chapters = activity.viewModel.state.value.viewerChapters!!
        viewer.onTransitionSelected(ChapterTransition.Next(chapters.currChapter, chapters.nextChapter))
        viewer.onTransitionSelected(ChapterTransition.Next(chapters.currChapter, null))
        val pages = chapters.currChapter.pages!!
        viewer.onPageSelected(pages.last(), allowPreload = true)
        viewer.onPageSelected(pages.first(), allowPreload = false)
        viewer.moveToPage(pages[3])
        viewer.moveToPage(eu.kanade.tachiyomi.ui.reader.model.ReaderPage(99))
        viewer.checkAllowPreload(null) shouldBe true
        viewer.checkAllowPreload(pages[0]) shouldBe true
        viewer.checkAllowPreload(chapters.nextChapter!!.pages?.firstOrNull() ?: pages[0]) shouldBe true
    }

    @Test
    fun scrollListenerHidesMenu() {
        activity.showMenu()
        viewer.recycler.scrollBy(0, 500)
        harness.settle()
        viewer.recycler.scrollBy(0, -500)
        harness.settle()
        repeat(10) { viewer.recycler.scrollBy(0, 2000) }
        harness.settle()
        viewer.refreshAdapter()
    }
}
