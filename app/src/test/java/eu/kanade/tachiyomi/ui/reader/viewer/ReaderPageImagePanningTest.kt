package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import android.graphics.RectF
import android.view.View
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Panning a zoomed still page left and right by a screen's width. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderPageImagePanningTest {
    private val rig = PageImageRig()
    private val target = slot<PointF>()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    // A still view with [remaining] pixels left to pan on each side, centred at 100,50.
    private fun still(remaining: Float, middle: PointF? = PointF(100f, 50f)): SubsamplingScaleImageView {
        val view = mockk<SubsamplingScaleImageView>(relaxed = true)
        every { view.getPanRemaining(any()) } answers { firstArg<RectF>().set(remaining, 0f, remaining, 0f) }
        every { view.center } returns middle
        every { view.width } returns 40
        every { view.scale } returns 2f
        every { view.animateCenter(capture(target)) } returns mockk(relaxed = true)
        return view
    }

    @Test
    fun panningNeedsRoomLeft() {
        val view = rig.view()
        view.canPanLeft() shouldBe false
        view.pageView = View(rig.context)
        view.canPanRight() shouldBe false
        view.pageView = still(remaining = 5f)
        view.canPanLeft() shouldBe true
        view.canPanRight() shouldBe true
        view.pageView = still(remaining = 0.5f)
        view.canPanLeft() shouldBe false
    }

    @Test
    fun panMovesByAScreen() {
        val view = rig.view()
        view.panLeft()
        val inner = still(remaining = 5f)
        view.pageView = inner
        view.panLeft()
        target.captured.x shouldBe 80f
        view.panRight()
        verify(exactly = 2) { inner.animateCenter(any()) }
    }

    @Test
    fun noCenterNoPan() {
        val view = rig.view()
        val inner = still(remaining = 5f, middle = null)
        view.pageView = inner
        view.panRight()
        verify(exactly = 0) { inner.animateCenter(any()) }
    }
}
