package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A drawable handed straight to the page view: still ones are subsampled, animated ones go through Coil. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderPageImageSourcesTest {
    private val rig = PageImageRig()
    private val config = ReaderPageImageView.Config(zoomDuration = 1)

    // An animated drawable that never animates.
    private class Frames : ColorDrawable(), Animatable {
        var started = false

        override fun start() {
            started = true
        }

        override fun stop() {
            started = false
        }

        override fun isRunning(): Boolean = started
    }

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun stillDrawableIsSubsampled() {
        val view = rig.view()
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        view.setImage(BitmapDrawable(rig.context.resources, bitmap), config)
        view.config shouldBe config
        val inner = view.pageView.shouldBeInstanceOf<SubsamplingScaleImageView>()
        inner.isVisible shouldBe true
        view.recycle()
        inner.isVisible shouldBe false
    }

    @Test
    fun animatedDrawableUsesCoil() {
        val view = rig.view()
        view.setImage(Frames(), config)
        view.pageView.shouldBeInstanceOf<AppCompatImageView>()
        rig.events shouldBe listOf("error:no image")
        view.recycle()
        view.pageView?.isVisible shouldBe false
    }
}
