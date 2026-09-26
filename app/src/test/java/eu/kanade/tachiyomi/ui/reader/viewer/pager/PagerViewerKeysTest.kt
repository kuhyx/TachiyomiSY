package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.setting.readWithVolumeKeys
import eu.kanade.tachiyomi.ui.reader.setting.readWithVolumeKeysInverted
import eu.kanade.tachiyomi.ui.reader.showMenu
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A pointer-sourced scroll of [vscroll] (negative scrolls down). */
internal fun scrollEvent(
    vscroll: Float,
    source: Int = InputDevice.SOURCE_MOUSE,
    action: Int = MotionEvent.ACTION_SCROLL,
): MotionEvent {
    val properties = MotionEvent.PointerProperties().apply { id = 0 }
    val coords = MotionEvent.PointerCoords().apply { setAxisValue(MotionEvent.AXIS_VSCROLL, vscroll) }
    return MotionEvent.obtain(
        0L,
        0L,
        action,
        1,
        arrayOf(properties),
        arrayOf(coords),
        0,
        0,
        1f,
        1f,
        0,
        0,
        source,
        0,
    )
}

@RunWith(RobolectricTestRunner::class)
internal class PagerViewerKeysTest {

    // Compose runs on a test clock: an animating page spinner never lets an auto-advancing clock idle.
    @get:Rule
    val compose = createEmptyComposeRule()

    private val harness = ReaderActivityHarness(pageCount = 6)
    private lateinit var activity: ReaderActivity
    private lateinit var viewer: PagerViewer

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        harness.start()
        activity = harness.launch().get()
        viewer = activity.viewModel.state.value.viewer as PagerViewer
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    private fun press(code: Int, meta: Int = 0): Boolean {
        val down = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, code, 0, meta)
        val up = KeyEvent(0L, 0L, KeyEvent.ACTION_UP, code, 0, meta)
        viewer.handleKeyEvent(down)
        return viewer.handleKeyEvent(up)
    }

    @Test
    fun arrowsMoveThePager() {
        val start = viewer.pager.currentItem
        press(KeyEvent.KEYCODE_DPAD_RIGHT) shouldBe true
        press(KeyEvent.KEYCODE_DPAD_LEFT) shouldBe true
        viewer.pager.currentItem shouldBe start
        press(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_CTRL_ON) shouldBe true
        press(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.META_CTRL_ON) shouldBe true
        press(KeyEvent.KEYCODE_DPAD_DOWN) shouldBe true
        press(KeyEvent.KEYCODE_PAGE_UP) shouldBe true
        press(KeyEvent.KEYCODE_PAGE_DOWN) shouldBe true
        press(KeyEvent.KEYCODE_DPAD_UP) shouldBe true
        press(KeyEvent.KEYCODE_A) shouldBe false
    }

    @Test
    fun menuKeyToggles() {
        press(KeyEvent.KEYCODE_MENU) shouldBe true
        activity.viewModel.state.value.menuVisible shouldBe true
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
    fun wheelScrollsPages() {
        viewer.handleGenericMotionEvent(scrollEvent(-1f)) shouldBe true
        viewer.handleGenericMotionEvent(scrollEvent(1f)) shouldBe true
        viewer.handleGenericMotionEvent(scrollEvent(1f, source = InputDevice.SOURCE_KEYBOARD)) shouldBe false
        viewer.handleGenericMotionEvent(scrollEvent(1f, action = MotionEvent.ACTION_HOVER_MOVE)) shouldBe false
    }

    @Test
    fun edgesStopMoving() {
        repeat(20) { viewer.moveRight() }
        val last = viewer.pager.currentItem
        last shouldBe viewer.adapter.count - 1
        viewer.moveRight()
        viewer.pager.currentItem shouldBe last
        repeat(20) { viewer.moveLeft() }
        viewer.pager.currentItem shouldBe 0
        viewer.moveLeft()
        viewer.moveUp()
        viewer.moveDown()
    }
}
