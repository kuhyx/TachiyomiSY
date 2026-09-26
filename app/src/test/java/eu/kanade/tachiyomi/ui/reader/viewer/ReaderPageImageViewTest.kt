package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.test.core.app.ApplicationProvider
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.AnimationBuilder
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView.Config
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView.ZoomStartPosition
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class ReaderPageImageViewTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val view = ReaderPageImageView(context)
    private val builder = mockk<AnimationBuilder>(relaxed = true)

    private fun laidOutSsiv(width: Int = 200, height: Int = 100, ready: Boolean = true): SubsamplingScaleImageView {
        val ssiv = mockk<SubsamplingScaleImageView>(relaxed = true)
        every { ssiv.isReady } returns ready
        every { ssiv.sWidth } returns width
        every { ssiv.sHeight } returns height
        every { ssiv.scale } returns 1f
        every { ssiv.minScale } returns 1f
        every { ssiv.height } returns 100
        every { ssiv.handler } returns Handler(Looper.getMainLooper())
        every { ssiv.center } returns PointF(1f, 1f)
        every { ssiv.animateScaleAndCenter(any(), any()) } returns builder
        every { ssiv.animateCenter(any()) } returns builder
        every { builder.withDuration(any()) } returns builder
        every { builder.withEasing(any()) } returns builder
        every { builder.withInterruptible(any()) } returns builder
        return ssiv
    }

    private fun zoomAfterDelay() = ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)

    @Test
    fun callbacksAreOptional() {
        view.onImageLoaded()
        view.onImageLoadError(null)
        view.onScaleChanged(2f)
        view.onViewClicked()
        val calls = mutableListOf<String>()
        view.onImageLoaded = { calls += "loaded" }
        view.onImageLoadError = { calls += "error" }
        view.onScaleChanged = { calls += "scale:$it" }
        view.onViewClicked = { calls += "click" }
        view.onImageLoaded()
        view.onImageLoadError(IllegalStateException())
        view.onScaleChanged(2f)
        view.onViewClicked()
        calls shouldBe listOf("loaded", "error", "scale:2.0", "click")
    }

    @Test
    fun landscapeZoomNeedsEverything() {
        val ssiv = laidOutSsiv()
        with(view) {
            ssiv.landscapeZoom(true)
            view.config = Config(zoomDuration = 1, landscapeZoom = false)
            ssiv.landscapeZoom(true)
            view.config = Config(zoomDuration = 1, minimumScaleType = SCALE_TYPE_CENTER_CROP, landscapeZoom = true)
            ssiv.landscapeZoom(true)
            view.config = Config(zoomDuration = 1, landscapeZoom = true)
            laidOutSsiv(width = 50).landscapeZoom(true)
            val zoomed = laidOutSsiv()
            every { zoomed.scale } returns 2f
            zoomed.landscapeZoom(true)
        }
        zoomAfterDelay()
        verify(exactly = 0) { builder.start() }
    }

    @Test
    fun landscapeZoomStartsFromEdges() {
        for (position in ZoomStartPosition.entries) {
            view.config = Config(zoomDuration = 1, zoomStartPosition = position, landscapeZoom = true)
            with(view) {
                laidOutSsiv().landscapeZoom(true)
                laidOutSsiv().landscapeZoom(false)
            }
        }
        val gone = laidOutSsiv()
        every { gone.animateScaleAndCenter(any(), any()) } returns null
        with(view) { gone.landscapeZoom(true) }
        zoomAfterDelay()
        verify(exactly = 6) { builder.start() }
    }

    @Test
    fun setupZoomCentersByConfig() {
        val ssiv = laidOutSsiv()
        with(view) {
            ZoomStartPosition.entries.forEach { ssiv.setupZoom(Config(zoomDuration = 1, zoomStartPosition = it)) }
            ssiv.setupZoom(null)
        }
        verify(exactly = 3) { ssiv.setScaleAndCenter(1f, any()) }
        verify(exactly = 4) { ssiv.maxScale = MAX_ZOOM_SCALE }
        with(view) { 3.getSystemScaledDuration() shouldBe 3 }
    }

    @Test
    fun pageSelectionZoomsOrWaits() {
        view.onPageSelected(true)
        view.pageView = AppCompatImageView(context)
        view.onPageSelected(true)
        view.pageView = laidOutSsiv()
        view.onPageSelected(true)
        val waiting = laidOutSsiv(ready = false)
        val listener = slot<SubsamplingScaleImageView.OnImageEventListener>()
        every { waiting.setOnImageEventListener(capture(listener)) } returns Unit
        view.pageView = waiting
        view.config = Config(zoomDuration = 1, landscapeZoom = true)
        var loaded = false
        var failed = false
        view.onImageLoaded = { loaded = true }
        view.onImageLoadError = { failed = true }
        view.onPageSelected(false)
        listener.captured.onReady()
        listener.captured.onImageLoadError(IllegalStateException())
        loaded shouldBe true
        failed shouldBe true
    }

    @Test
    fun panningNeedsSubsampling() {
        view.canPanLeft() shouldBe false
        view.panLeft()
        val ssiv = laidOutSsiv()
        every { ssiv.getPanRemaining(any()) } answers { firstArg<RectF>().set(5f, 0f, 0f, 0f) }
        view.pageView = ssiv
        view.canPanLeft() shouldBe true
        view.canPanRight() shouldBe false
        view.panLeft()
        view.panRight()
        every { ssiv.center } returns null
        view.panRight()
        verify(exactly = 2) { builder.start() }
    }

    @Test
    fun recycleByViewKind() {
        view.recycle()
        val ssiv = laidOutSsiv()
        view.pageView = ssiv
        view.recycle()
        verify { ssiv.recycle() }
        view.pageView = AppCompatImageView(context)
        view.recycle()
        view.pageView!!.visibility shouldBe View.GONE
        Config(zoomDuration = 1).minimumScaleType shouldBe SCALE_TYPE_CENTER_INSIDE
    }
}
