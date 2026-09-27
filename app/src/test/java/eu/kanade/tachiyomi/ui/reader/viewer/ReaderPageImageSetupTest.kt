package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import coil3.asImage
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.chrisbanes.photoview.OnScaleChangedListener
import com.github.chrisbanes.photoview.PhotoView
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonSubsamplingImageView
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

/** How a page image view prepares and fills its inner still or animated image view. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderPageImageSetupTest {
    private val rig = PageImageRig()
    private val config = ReaderPageImageView.Config(zoomDuration = 100)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun bitmap(): Bitmap = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888)

    private fun png(): Buffer {
        val out = ByteArrayOutputStream()
        bitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
        return Buffer().write(out.toByteArray())
    }

    @Test
    fun stillViewIsKeptAndWired() {
        val view = rig.view()
        view.prepareNonAnimatedImageView()
        val inner = view.pageView.shouldBeInstanceOf<SubsamplingScaleImageView>()
        view.prepareNonAnimatedImageView()
        view.pageView shouldBeSameInstanceAs inner
        inner.stateEvents().onScaleChanged(2f, 0)
        inner.stateEvents().onCenterChanged(null, 0)
        inner.performClick()
        rig.events shouldBe listOf("scale:2.0", "click")
        rig.view(webtoon = true).apply { prepareNonAnimatedImageView() }.pageView
            .shouldBeInstanceOf<WebtoonSubsamplingImageView>()
    }

    @Test
    fun bitmapShowsAndReports() {
        val view = rig.view()
        view.prepareNonAnimatedImageView()
        val inner = view.setNonAnimatedImage(BitmapDrawable(rig.context.resources, bitmap()), config)!!
        inner.visibility shouldBe View.VISIBLE
        inner.imageEvents().onReady()
        inner.imageEvents().onImageLoadError(IllegalStateException("bad"))
        rig.events shouldBe listOf("loaded", "error:bad")
    }

    @Test
    fun streamsDecodeInPlace() {
        val view = rig.view()
        view.prepareNonAnimatedImageView()
        view.setNonAnimatedImage(png(), config)!!.visibility shouldBe View.VISIBLE
        rig.base.alwaysDecodeLongStripWithSSIV.set(true)
        val strip = rig.view(webtoon = true)
        strip.prepareNonAnimatedImageView()
        strip.setNonAnimatedImage(png(), config)!!.visibility shouldBe View.VISIBLE
    }

    @Test
    fun longStripGoesThroughCoil() {
        rig.image = bitmap().asImage()
        val strip = rig.view(webtoon = true)
        strip.prepareNonAnimatedImageView()
        strip.setNonAnimatedImage(png(), config)!!.visibility shouldBe View.VISIBLE
        rig.image = null
        val failing = rig.view(webtoon = true)
        failing.prepareNonAnimatedImageView()
        failing.setNonAnimatedImage(png(), config)
        rig.events shouldBe listOf("error:no image")
    }

    @Test
    fun unknownDataIsRejected() {
        val view = rig.view()
        view.setNonAnimatedImage(Any(), config) shouldBe null
        view.prepareNonAnimatedImageView()
        shouldThrow<IllegalArgumentException> { view.setNonAnimatedImage(Any(), config) }
    }

    @Test
    fun animatedViewDoubleTaps() {
        val view = rig.view()
        view.prepareAnimatedImageView()
        val photo = view.pageView.shouldBeInstanceOf<PhotoView>()
        view.prepareAnimatedImageView()
        view.pageView shouldBeSameInstanceAs photo
        val detector = photo.attacher.read("mGestureDetector")!!
        val doubleTap = detector.read("mDoubleTapListener", GestureDetector::class.java)
            as GestureDetector.OnDoubleTapListener
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 1f, 1f, 0)
        doubleTap.onDoubleTap(event) shouldBe true
        photo.setScale(2f)
        doubleTap.onDoubleTap(event) shouldBe true
        doubleTap.onSingleTapConfirmed(event)
        (photo.attacher.read("mScaleChangeListener") as OnScaleChangedListener).onScaleChange(1f, 0f, 0f)
        rig.events.first() shouldBe "click"
        rig.events.last().startsWith("scale:") shouldBe true
    }

    @Test
    fun animatedImageLoadsOrFails() {
        rig.image = AnimationDrawable().asImage()
        val view = rig.view()
        view.setAnimatedImage(Any(), config) shouldBe null
        view.prepareAnimatedImageView()
        view.setAnimatedImage(Any(), config)!!.visibility shouldBe View.VISIBLE
        rig.image = null
        val webtoon = rig.view(webtoon = true)
        webtoon.prepareAnimatedImageView()
        webtoon.pageView.shouldBeInstanceOf<AppCompatImageView>()
        webtoon.setAnimatedImage(Any(), config)
        rig.events shouldBe listOf("loaded", "error:no image")
    }
}

// A private field of this object, declared on [owner] (its own class unless given).
private fun Any.read(name: String, owner: Class<*> = javaClass): Any? {
    val field = owner.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this)
}
