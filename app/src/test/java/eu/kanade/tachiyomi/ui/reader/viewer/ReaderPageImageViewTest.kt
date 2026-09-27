package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView.ZoomStartPosition
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

/** The page image view's callbacks, its page-selected zoom and its zoom setup. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderPageImageViewTest {
    private val rig = PageImageRig()
    private val scheduled = slot<Runnable>()
    private val mainHandler = mockk<Handler> { every { postDelayed(capture(scheduled), any<Long>()) } returns true }

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    // A still view whose image is [width] x 10 and not yet zoomed in.
    private fun still(width: Int): SubsamplingScaleImageView = mockk(relaxed = true) {
        every { sWidth } returns width
        every { sHeight } returns 10
        every { scale } returns 1f
        every { minScale } returns 1f
        every { handler } returns mainHandler
        every { center } returns PointF(1f, 1f)
    }

    private fun ReaderPageImageView.zoomWith(
        position: ZoomStartPosition,
        forward: Boolean,
        view: SubsamplingScaleImageView,
    ) {
        config = ReaderPageImageView.Config(zoomDuration = 1, landscapeZoom = true, zoomStartPosition = position)
        view.landscapeZoom(forward)
    }

    @Test
    fun callbacksAreOptional() {
        val bare = ReaderPageImageView(rig.context)
        bare.onImageLoaded()
        bare.onImageLoadError(null)
        bare.onScaleChanged(1f)
        bare.onViewClicked()
        val view = rig.view().apply { pageBackground = ColorDrawable() }
        view.onImageLoaded()
        view.background shouldBe view.pageBackground
        view.onImageLoadError(null)
        view.onScaleChanged(3f)
        view.onViewClicked()
        rig.events shouldBe listOf("loaded", "error:null", "scale:3.0", "click")
    }

    @Test
    fun selectionWaitsForTheImage() {
        val view = rig.view()
        view.onPageSelected(forward = true)
        view.prepareNonAnimatedImageView()
        val inner = view.pageView as SubsamplingScaleImageView
        view.onPageSelected(forward = true)
        inner.imageEvents().onReady()
        rig.events shouldBe listOf("loaded")
    }

    @Test
    fun notReadyErrorReachesTheView() {
        val view = rig.view()
        view.prepareNonAnimatedImageView()
        val inner = view.pageView as SubsamplingScaleImageView
        view.onPageSelected(forward = true)
        // The listener used to call its own onImageLoadError: a StackOverflowError instead of this.
        inner.imageEvents().onImageLoadError(IllegalStateException("decode"))
        rig.events shouldBe listOf("error:decode")
    }

    @Test
    fun readyImageZoomsAtOnce() {
        val view = rig.view()
        val inner = still(width = 20)
        every { inner.isReady } returns true
        view.pageView = inner
        view.config = ReaderPageImageView.Config(zoomDuration = 1, landscapeZoom = true)
        view.onPageSelected(forward = false)
        scheduled.captured.run()
        verify { inner.animateScaleAndCenter(any(), PointF(1f, 1f)) }
    }

    @Test
    fun landscapeZoomStartsAtAnEdge() {
        val view = rig.view()
        val inner = still(width = 20)
        with(view) {
            zoomWith(ZoomStartPosition.LEFT, forward = true, view = inner)
            scheduled.captured.run()
            zoomWith(ZoomStartPosition.LEFT, forward = false, view = inner)
            scheduled.captured.run()
            zoomWith(ZoomStartPosition.RIGHT, forward = true, view = inner)
            scheduled.captured.run()
            zoomWith(ZoomStartPosition.RIGHT, forward = false, view = inner)
            scheduled.captured.run()
        }
        verify(exactly = 2) { inner.animateScaleAndCenter(any(), PointF(0f, 0f)) }
        verify(exactly = 2) { inner.animateScaleAndCenter(any(), PointF(20f, 0f)) }
    }

    @Test
    fun onlyWideUnzoomedImagesZoom() {
        val view = rig.view()
        with(view) {
            still(width = 20).landscapeZoom(true)
            config = ReaderPageImageView.Config(zoomDuration = 1, landscapeZoom = false)
            still(width = 20).landscapeZoom(true)
            config = ReaderPageImageView.Config(zoomDuration = 1, landscapeZoom = true, minimumScaleType = 2)
            still(width = 20).landscapeZoom(true)
            config = ReaderPageImageView.Config(zoomDuration = 1, landscapeZoom = true)
            still(width = 5).landscapeZoom(true)
            val zoomed = still(width = 20).also { every { it.scale } returns 2f }
            zoomed.landscapeZoom(true)
        }
        scheduled.isCaptured shouldBe false
    }

    @Test
    fun zoomSetupCentersPerPosition() {
        val view = rig.view()
        val inner = still(width = 20)
        with(view) {
            inner.setupZoom(null)
            ZoomStartPosition.entries.forEach {
                inner.setupZoom(ReaderPageImageView.Config(zoomDuration = 1, zoomStartPosition = it))
            }
            7.getSystemScaledDuration() shouldBe 7
        }
        verify { inner.setScaleAndCenter(1f, PointF(0f, 0f)) }
        verify { inner.setScaleAndCenter(1f, PointF(20f, 0f)) }
        verify { inner.setScaleAndCenter(1f, PointF(1f, 1f)) }
        verify(exactly = 4) { inner.maxScale = 5f }
    }
}
