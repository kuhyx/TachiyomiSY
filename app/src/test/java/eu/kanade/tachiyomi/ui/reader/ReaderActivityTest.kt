package eu.kanade.tachiyomi.ui.reader

import android.app.assist.AssistContent
import android.content.Intent
import android.view.KeyEvent
import android.view.MotionEvent
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class ReaderActivityTest {

    private val harness = ReaderActivityHarness()

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun missingExtrasFinish() {
        val intent = Intent(harness.app, ReaderActivity::class.java)
        Robolectric.buildActivity(ReaderActivity::class.java, intent).setup().get().isFinishing shouldBe true
        val noChapter = Intent(harness.app, ReaderActivity::class.java).putExtra("manga", 10L)
        Robolectric.buildActivity(ReaderActivity::class.java, noChapter).setup().get().isFinishing shouldBe true
    }

    @Test
    fun launchLoadsChapter() {
        val controller = harness.launch(page = 1)
        val activity = controller.get()
        activity.isFinishing shouldBe false
        activity.viewModel.state.value.currentChapter!!.chapter.id shouldBe 2L
        activity.viewModel.state.value.viewer.shouldBeInstanceOf<R2LPagerViewer>()
        activity.menuToggleToast = mockk(relaxed = true)
        activity.readingModeToast = mockk(relaxed = true)
        controller.pause().stop().destroy()
    }

    @Test
    fun unknownMangaFinishes() {
        coEvery { harness.vm.getManga.await(10L) } returns null
        harness.launch()
        harness.settleUntil { ShadowToast.getTextOfLatestToast() == "Unknown err" }
    }

    @Test
    fun failedInitFinishes() {
        coEvery { harness.vm.getManga.await(10L) } throws IllegalStateException("db down")
        val activity = harness.launch().get()
        harness.settleUntil { ShadowToast.getTextOfLatestToast() == "db down" }
        activity.isFinishing shouldBe true
    }

    @Test
    fun keysLoadChapters() {
        val activity = harness.launch().get()
        activity.onKeyUp(KeyEvent.KEYCODE_N, null) shouldBe true
        harness.settle()
        activity.viewModel.state.value.currentChapter!!.chapter.id shouldBe 3L
        activity.onKeyUp(KeyEvent.KEYCODE_P, null) shouldBe true
        harness.settle()
        activity.viewModel.state.value.currentChapter!!.chapter.id shouldBe 2L
        activity.onKeyUp(KeyEvent.KEYCODE_A, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A)) shouldBe false
    }

    @Test
    fun viewerGetsKeysFirst() {
        val activity = harness.launch().get()
        val key = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)
        val motion = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_SCROLL, 0f, 0f, 0)
        activity.dispatchKeyEvent(key)
        activity.dispatchGenericMotionEvent(motion)
        val viewer = mockk<Viewer>(relaxed = true)
        every { viewer.handleKeyEvent(any()) } returns true
        every { viewer.handleGenericMotionEvent(any()) } returns true
        activity.viewModel.onViewerLoaded(viewer)
        activity.dispatchKeyEvent(key) shouldBe true
        activity.dispatchGenericMotionEvent(motion) shouldBe true
        activity.viewModel.onViewerLoaded(null)
        activity.dispatchKeyEvent(key) shouldBe false
        activity.dispatchGenericMotionEvent(motion) shouldBe false
    }

    @Test
    fun focusAndAssist() {
        val activity = harness.launch().get()
        activity.onWindowFocusChanged(false)
        activity.onWindowFocusChanged(true)
        val content = AssistContent()
        activity.assistUrl = null
        activity.onProvideAssistContent(content)
        content.webUri.shouldBeNull()
        activity.assistUrl = "https://example.org/c/2"
        activity.onProvideAssistContent(content)
        content.webUri.toString() shouldBe "https://example.org/c/2"
    }

    @Test
    fun menuVisibilityToggles() {
        val activity = harness.launch().get()
        activity.showMenu()
        activity.viewModel.state.value.menuVisible shouldBe true
        activity.showMenu()
        activity.toggleMenu()
        activity.viewModel.state.value.menuVisible shouldBe false
        activity.hideMenu()
        harness.vm.readerPreferences.fullscreen.set(false)
        activity.toggleMenu()
        activity.hideMenu()
        activity.viewModel.state.value.menuVisible shouldBe false
    }

    @Test
    fun finishNotifiesViewModel() {
        val activity = harness.launch().get()
        activity.finish()
        activity.isFinishing shouldBe true
    }
}
